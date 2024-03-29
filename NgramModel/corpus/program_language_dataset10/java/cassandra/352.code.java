package org.apache.cassandra.service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.cassandra.auth.AuthenticatedUser;
import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.auth.Resources;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.thrift.AuthenticationException;
import org.apache.cassandra.thrift.InvalidRequestException;
public class ClientState
{
    private static Logger logger = LoggerFactory.getLogger(ClientState.class);
    private AuthenticatedUser user;
    private String keyspace;
    private final List<Object> resource = new ArrayList<Object>();
    public ClientState()
    {
        reset();
    }
    public String getKeyspace()
    {
        return keyspace;
    }
    public void setKeyspace(String ks)
    {
        keyspace = ks;
    }
    public String getSchedulingValue()
    {
        switch(DatabaseDescriptor.getRequestSchedulerId())
        {
            case keyspace: return keyspace;
        }
        return "default";
    }
    public void login(Map<? extends CharSequence,? extends CharSequence> credentials) throws AuthenticationException
    {
        AuthenticatedUser user = DatabaseDescriptor.getAuthenticator().authenticate(credentials);
        if (logger.isDebugEnabled())
            logger.debug("logged in: {}", user);
        this.user = user;
    }
    public void logout()
    {
        if (logger.isDebugEnabled())
            logger.debug("logged out: {}", user);
        reset();
    }
    private void resourceClear()
    {
        resource.clear();
        resource.add(Resources.ROOT);
        resource.add(Resources.KEYSPACES);
    }
    public void reset()
    {
        user = DatabaseDescriptor.getAuthenticator().defaultUser();
        keyspace = null;
        resourceClear();
    }
    public void hasKeyspaceListAccess(Permission perm) throws InvalidRequestException
    {
        validateLogin();
        resourceClear();
        Set<Permission> perms = DatabaseDescriptor.getAuthority().authorize(user, resource);
        hasAccess(user, perms, perm, resource);
    }
    public void hasColumnFamilyListAccess(Permission perm) throws InvalidRequestException
    {
        validateLogin();
        validateKeyspace();
        resourceClear();
        resource.add(keyspace);
        Set<Permission> perms = DatabaseDescriptor.getAuthority().authorize(user, resource);
        hasAccess(user, perms, perm, resource);
    }
    public void hasColumnFamilyAccess(String columnFamily, Permission perm) throws InvalidRequestException
    {
        validateLogin();
        validateKeyspace();
        resourceClear();
        resource.add(keyspace);
        resource.add(columnFamily);
        Set<Permission> perms = DatabaseDescriptor.getAuthority().authorize(user, resource);
        hasAccess(user, perms, perm, resource);
    }
    private void validateLogin() throws InvalidRequestException
    {
        if (user == null)
            throw new InvalidRequestException("You have not logged in");
    }
    private void validateKeyspace() throws InvalidRequestException
    {
        if (keyspace == null)
            throw new InvalidRequestException("You have not set a keyspace for this session");
    }
    private static void hasAccess(AuthenticatedUser user, Set<Permission> perms, Permission perm, List<Object> resource) throws InvalidRequestException
    {
        if (perms.contains(perm))
            return;
        throw new InvalidRequestException(String.format("%s does not have permission %s for %s",
                                                        user,
                                                        perm,
                                                        Resources.toString(resource)));
    }
}
