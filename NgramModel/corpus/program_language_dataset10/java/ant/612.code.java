package org.apache.tools.ant.types;
import java.lang.reflect.Constructor;
import java.security.UnresolvedPermission;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitException;
public class Permissions {
    private List grantedPermissions = new LinkedList();
    private List revokedPermissions = new LinkedList();
    private java.security.Permissions granted = null;
    private SecurityManager origSm = null;
    private boolean active = false;
    private boolean delegateToOldSM;
    private static final Class[] PARAMS = {String.class, String.class};
    public Permissions() {
        this(false);
    }
    public Permissions(boolean delegateToOldSM) {
        this.delegateToOldSM = delegateToOldSM;
    }
    public void addConfiguredGrant(Permissions.Permission perm) {
        grantedPermissions.add(perm);
    }
    public void addConfiguredRevoke(Permissions.Permission perm) {
        revokedPermissions.add(perm);
    }
    public synchronized void setSecurityManager() throws BuildException {
        origSm = System.getSecurityManager();
        init();
        System.setSecurityManager(new MySM());
        active = true;
    }
    private void init() throws BuildException {
        granted = new java.security.Permissions();
        for (Iterator i = revokedPermissions.listIterator(); i.hasNext();) {
            Permissions.Permission p = (Permissions.Permission) i.next();
            if (p.getClassName() == null) {
                throw new BuildException("Revoked permission " + p + " does not contain a class.");
            }
        }
        for (Iterator i = grantedPermissions.listIterator(); i.hasNext();) {
            Permissions.Permission p = (Permissions.Permission) i.next();
            if (p.getClassName() == null) {
                throw new BuildException("Granted permission " + p
                        + " does not contain a class.");
            } else {
                java.security.Permission perm = createPermission(p);
                granted.add(perm);
            }
        }
        granted.add(new java.net.SocketPermission("localhost:1024-", "listen"));
        granted.add(new java.util.PropertyPermission("java.version", "read"));
        granted.add(new java.util.PropertyPermission("java.vendor", "read"));
        granted.add(new java.util.PropertyPermission("java.vendor.url", "read"));
        granted.add(new java.util.PropertyPermission("java.class.version", "read"));
        granted.add(new java.util.PropertyPermission("os.name", "read"));
        granted.add(new java.util.PropertyPermission("os.version", "read"));
        granted.add(new java.util.PropertyPermission("os.arch", "read"));
        granted.add(new java.util.PropertyPermission("file.encoding", "read"));
        granted.add(new java.util.PropertyPermission("file.separator", "read"));
        granted.add(new java.util.PropertyPermission("path.separator", "read"));
        granted.add(new java.util.PropertyPermission("line.separator", "read"));
        granted.add(new java.util.PropertyPermission("java.specification.version", "read"));
        granted.add(new java.util.PropertyPermission("java.specification.vendor", "read"));
        granted.add(new java.util.PropertyPermission("java.specification.name", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.specification.version", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.specification.vendor", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.specification.name", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.version", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.vendor", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.name", "read"));
    }
    private java.security.Permission createPermission(
            Permissions.Permission permission) {
        try {
            Class clazz = Class.forName(permission.getClassName());
            String name = permission.getName();
            String actions = permission.getActions();
            Constructor ctr = clazz.getConstructor(PARAMS);
            return (java.security.Permission) ctr.newInstance(new Object[] {
                    name, actions });
        } catch (Exception e) {
            return new UnresolvedPermission(permission.getClassName(),
                    permission.getName(), permission.getActions(), null);
        }
    }
    public synchronized void restoreSecurityManager() {
        active = false;
        System.setSecurityManager(origSm);
    }
    private class MySM extends SecurityManager {
        public void checkExit(int status) {
            java.security.Permission perm = new java.lang.RuntimePermission("exitVM", null);
            try {
                checkPermission(perm);
            } catch (SecurityException e) {
                throw new ExitException(e.getMessage(), status);
            }
        }
        public void checkPermission(java.security.Permission perm) {
            if (active) {
                if (delegateToOldSM && !perm.getName().equals("exitVM")) {
                    boolean permOK = false;
                    if (granted.implies(perm)) {
                        permOK = true;
                    }
                    checkRevoked(perm);
                    if (!permOK && origSm != null) {
                        origSm.checkPermission(perm);
                    }
                }  else {
                    if (!granted.implies(perm)) {
                        throw new SecurityException("Permission " + perm + " was not granted.");
                    }
                    checkRevoked(perm);
                }
            }
        }
        private void checkRevoked(java.security.Permission perm) {
            for (Iterator i = revokedPermissions.listIterator(); i.hasNext();) {
                if (((Permissions.Permission) i.next()).matches(perm)) {
                    throw new SecurityException("Permission " + perm + " was revoked.");
                }
            }
        }
    }
    public static class Permission {
        private String className;
        private String name;
        private String actionString;
        private Set actions;
        public void setClass(String aClass) {
                className = aClass.trim();
        }
        public String getClassName() {
            return className;
        }
        public void setName(String aName) {
            name = aName.trim();
        }
        public String getName() {
            return name;
        }
        public void setActions(String actions) {
            actionString = actions;
            if (actions.length() > 0) {
                this.actions = parseActions(actions);
            }
        }
        public String getActions() {
            return actionString;
        }
        boolean matches(java.security.Permission perm) {
            if (!className.equals(perm.getClass().getName())) {
                return false;
            }
            if (name != null) {
                if (name.endsWith("*")) {
                    if (!perm.getName().startsWith(name.substring(0, name.length() - 1))) {
                        return false;
                    }
                } else {
                    if (!name.equals(perm.getName())) {
                        return false;
                    }
                }
            }
            if (actions != null) {
                Set as = parseActions(perm.getActions());
                int size = as.size();
                as.removeAll(actions);
                if (as.size() == size) {
                    return false;
                }
            }
            return true;
        }
        private Set parseActions(String actions) {
            Set result = new HashSet();
            StringTokenizer tk = new StringTokenizer(actions, ",");
            while (tk.hasMoreTokens()) {
                String item = tk.nextToken().trim();
                if (!item.equals("")) {
                    result.add(item);
                }
            }
            return result;
        }
        public String toString() {
            return ("Permission: " + className + " (\"" + name + "\", \"" + actions + "\")");
        }
    }
}