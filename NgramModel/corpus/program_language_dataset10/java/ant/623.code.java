package org.apache.tools.ant.types;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
public class TarFileSet extends ArchiveFileSet {
    private boolean userNameSet;
    private boolean groupNameSet;
    private boolean userIdSet;
    private boolean groupIdSet;
    private String userName = "";
    private String groupName = "";
    private int    uid;
    private int    gid;
    public TarFileSet() {
        super();
    }
    protected TarFileSet(FileSet fileset) {
        super(fileset);
    }
    protected TarFileSet(TarFileSet fileset) {
        super(fileset);
    }
    public void setUserName(String userName) {
        checkTarFileSetAttributesAllowed();
        userNameSet = true;
        this.userName = userName;
    }
    public String getUserName() {
        if (isReference()) {
            return ((TarFileSet) getCheckedRef()).getUserName();
        }
        return userName;
    }
    public boolean hasUserNameBeenSet() {
        return userNameSet;
    }
    public void setUid(int uid) {
        checkTarFileSetAttributesAllowed();
        userIdSet = true;
        this.uid = uid;
    }
    public int getUid() {
        if (isReference()) {
            return ((TarFileSet) getCheckedRef()).getUid();
        }
        return uid;
    }
    public boolean hasUserIdBeenSet() {
        return userIdSet;
    }
    public void setGroup(String groupName) {
        checkTarFileSetAttributesAllowed();
        groupNameSet = true;
        this.groupName = groupName;
    }
    public String getGroup() {
        if (isReference()) {
            return ((TarFileSet) getCheckedRef()).getGroup();
        }
        return groupName;
    }
    public boolean hasGroupBeenSet() {
        return groupNameSet;
    }
    public void setGid(int gid) {
        checkTarFileSetAttributesAllowed();
        groupIdSet = true;
        this.gid = gid;
    }
    public int getGid() {
        if (isReference()) {
            return ((TarFileSet) getCheckedRef()).getGid();
        }
        return gid;
    }
    public boolean hasGroupIdBeenSet() {
        return groupIdSet;
    }
    protected ArchiveScanner newArchiveScanner() {
        TarScanner zs = new TarScanner();
        return zs;
    }
    public void setRefid(Reference r) throws BuildException {
        if (userNameSet || userIdSet || groupNameSet || groupIdSet) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }
    protected AbstractFileSet getRef(Project p) {
        dieOnCircularReference(p);
        Object o = getRefid().getReferencedObject(p);
        if (o instanceof TarFileSet) {
            return (AbstractFileSet) o;
        } else if (o instanceof FileSet) {
            TarFileSet zfs = new TarFileSet((FileSet) o);
            configureFileSet(zfs);
            return zfs;
        } else {
            String msg = getRefid().getRefId() + " doesn\'t denote a tarfileset or a fileset";
            throw new BuildException(msg);
        }
    }
    protected void configureFileSet(ArchiveFileSet zfs) {
        super.configureFileSet(zfs);
        if (zfs instanceof TarFileSet) {
            TarFileSet tfs = (TarFileSet) zfs;
            tfs.setUserName(userName);
            tfs.setGroup(groupName);
            tfs.setUid(uid);
            tfs.setGid(gid);
        }
    }
    public Object clone() {
        if (isReference()) {
            return ((TarFileSet) getRef(getProject())).clone();
        } else {
            return super.clone();
        }
    }
    private void checkTarFileSetAttributesAllowed() {
        if (getProject() == null
            || (isReference()
                && (getRefid().getReferencedObject(
                        getProject())
                    instanceof TarFileSet))) {
            checkAttributesAllowed();
        }
    }
}
