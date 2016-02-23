package com.apifest.oauth20.persistence.cassandra;

import com.apifest.oauth20.*;
import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Giovanni Baleani on 23/02/2016.
 */
public class CassandraDBManager implements DBManager {

    protected static Logger log = LoggerFactory.getLogger(DBManager.class);

    protected static final String KEYSPACE_NAME = "apifest";
    protected static final String CLIENTS_TABLE_NAME = "clients";
    protected static final String AUTH_CODE_TABLE_NAME = "auth_codes";
    protected static final String ACCESS_TOKEN_TABLE_NAME = "access_tokens";
    protected static final String SCOPE_TABLE_NAME = "scopes";


    protected static final String SCOPE_TABLE_CQL =
            "CREATE TABLE IF NOT EXISTS "+KEYSPACE_NAME+"."+SCOPE_TABLE_NAME+" (" +
            " scope text," +
            " description text," +
            " cc_expires_in int," +
            " pass_expires_in int," +
            " refresh_expires_in int," +
            " PRIMARY KEY (scope)" +
            ");";

    protected static final String CLIENTS_TABLE_CQL =
            "CREATE TABLE IF NOT EXISTS "+KEYSPACE_NAME+"."+CLIENTS_TABLE_NAME+" (" +
            " client_id text," +
            " client_secret text," +
            " scope text," +
            " name text," +
            " created timestamp," +
            " uri text," +
            " descr text," +
            " type int," +
            " status int," +
            " details MAP<text, text>," +
            " PRIMARY KEY (client_id)" +
            ");";


    private Cluster cluster;
    private Session session;
    private void connect(String node) {
        cluster = Cluster.builder()
                .addContactPoint(node)
                .build();
        Metadata metadata = cluster.getMetadata();
        System.out.printf("Connected to cluster: %s\n",
                metadata.getClusterName());
        for ( Host host : metadata.getAllHosts() ) {
            System.out.printf("Datacenter: %s; Host: %s; Rack: %s\n",
                    host.getDatacenter(), host.getAddress(), host.getRack());
        }
    }
    public CassandraDBManager() {
        connect("172.16.11.100");
        session = cluster.connect(KEYSPACE_NAME);
        session.execute(SCOPE_TABLE_CQL);
        session.execute(CLIENTS_TABLE_CQL);
    }










    @Override
    public void storeAccessToken(AccessToken accessToken) {
        //TODO: persist accessToken obj into ACCESS_TOKEN_TABLE_NAME
        // N.B. accessToken.token is the key... byt "findAccessTokenByRefreshToken" need to lookup by refreshToken and clientId
    }

    @Override
    public AccessToken findAccessTokenByRefreshToken(String refreshToken, String clientId) {
        // MongoDB performa a lookup into ACCESS_TOKEN_TABLE_NAME
        // using refreshToken and clientId as lookup key
        return null;
    }

    @Override
    public void updateAccessTokenValidStatus(String accessToken, boolean valid) {
        // ACCESS_TOKEN_TABLE_NAME
    }

    @Override
    public AccessToken findAccessToken(String accessToken) {
        // ACCESS_TOKEN_TABLE_NAME
        return null;
    }

    @Override
    public void removeAccessToken(String accessToken) {
        // delete from ACCESS_TOKEN_TABLE_NAME using accessToken as key
    }

    @Override
    public List<AccessToken> getAccessTokenByUserIdAndClientApp(String userId, String clientId) {
        // ACCESS_TOKEN_TABLE_NAME
        return null;
    }





    @Override
    public void storeAuthCode(AuthCode authCode) {
        //TODO: persist authCode obj into AUTH_CODE_TABLE_NAME
    }

    @Override
    public void updateAuthCodeValidStatus(String authCode, boolean valid) {
        //TODO: update valid flag by authCode key into AUTH_CODE_TABLE_NAME
        // 1. find 1 record by authCode
        // 2. update valid flag
    }

    @Override
    public AuthCode findAuthCode(String authCode, String redirectUri) {
        // AUTH_CODE_TABLE_NAME
        return null;
    }





    @Override
    public boolean storeScope(Scope scope) {
        try {
            Insert stmt = QueryBuilder.insertInto(KEYSPACE_NAME, SCOPE_TABLE_NAME)
                    .value("scope", scope.getScope())
                    .value("description", scope.getDescription())
                    .value("cc_expires_in", scope.getCcExpiresIn())
                    .value("pass_expires_in", scope.getPassExpiresIn())
                    .value("refresh_expires_in", scope.getRefreshExpiresIn())
                    ;
            session.execute(stmt);
        } catch(Throwable e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public List<Scope> getAllScopes() {
        Select stmt = QueryBuilder.select().from(KEYSPACE_NAME, SCOPE_TABLE_NAME);
        ResultSet rs = session.execute(stmt);
        List<Scope> list = new ArrayList<Scope>();
        for (Row row : rs) {
            Scope scope = new Scope();
            scope.setScope(row.getString("scope"));
            scope.setDescription(row.getString("description"));
            scope.setCcExpiresIn(row.getInt("cc_expires_in"));
            scope.setPassExpiresIn(row.getInt("pass_expires_in"));
            scope.setRefreshExpiresIn(row.getInt("refresh_expires_in"));
            list.add(scope);
        }
        return list;
    }

    @Override
    public Scope findScope(String scopeName) {
        try {
            Select.Where stmt = QueryBuilder.select().from(KEYSPACE_NAME, SCOPE_TABLE_NAME)
                    .where(QueryBuilder.eq("scope", scopeName));
            ResultSet rs = session.execute(stmt);
            Iterator<Row> iter = rs.iterator();
            if(iter.hasNext()) {
                Scope scope = new Scope();
                Row row = iter.next();
                scope.setScope(row.getString("scope"));
                scope.setDescription(row.getString("description"));
                scope.setCcExpiresIn(row.getInt("cc_expires_in"));
                scope.setPassExpiresIn(row.getInt("pass_expires_in"));
                scope.setRefreshExpiresIn(row.getInt("refresh_expires_in"));
                return scope;
            }
        } catch(Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return null;
    }

    @Override
    public boolean deleteScope(String scopeName) {
        try {
            Delete.Where stmt = QueryBuilder.delete().from(KEYSPACE_NAME, SCOPE_TABLE_NAME)
                    .where(QueryBuilder.eq("scope", scopeName));
            session.execute(stmt);
        } catch(Throwable e) {
            return false;
        }
        return true;
    }



    @Override
    public boolean validClient(String clientId, String clientSecret) {
        try {
            Select.Where stmt = QueryBuilder.select("client_id", "client_secret", "status")
                    .from(KEYSPACE_NAME, CLIENTS_TABLE_NAME)
                    .where(QueryBuilder.eq("client_id", clientId));
            ResultSet rs = session.execute(stmt);
            Iterator<Row> iter = rs.iterator();
            if(iter.hasNext()) {
                Row row = iter.next();
                boolean ret = (row.getString("client_secret").equals(clientSecret)
                        && String.valueOf(ClientCredentials.ACTIVE_STATUS).equals(row.getInt("status")));
                return ret;
            }
        } catch(Throwable e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
    @Override
    public ClientCredentials findClientCredentials(String clientId) {
        try {
            Select.Where stmt = QueryBuilder.select().from(KEYSPACE_NAME, CLIENTS_TABLE_NAME)
                    .where(QueryBuilder.eq("client_id", clientId));
            ResultSet rs = session.execute(stmt);
            Iterator<Row> iter = rs.iterator();
            if(iter.hasNext()) {
                Row row = iter.next();
                ClientCredentials app = new ClientCredentials();
                app.setId(row.getString("client_id"));
                app.setSecret(row.getString("client_secret"));
                app.setScope(row.getString("scope"));
                app.setName(row.getString("name"));
                app.setCreated(row.getTimestamp("created").getTime());
                app.setUri(row.getString("uri"));
                app.setDescr(row.getString("descr"));
                app.setType(row.getInt("type"));
                app.setStatus(row.getInt("status"));
                app.setApplicationDetails(row.getMap("details", String.class, String.class));
                return app;
            }
        } catch(Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return null;
    }

    @Override
    public void storeClientCredentials(ClientCredentials clientCreds) {
        try {
            Insert stmt = QueryBuilder.insertInto(KEYSPACE_NAME, CLIENTS_TABLE_NAME)
                    .value("client_id", clientCreds.getId())
                    .value("client_secret", clientCreds.getSecret())
                    .value("scope", clientCreds.getScope())
                    .value("name", clientCreds.getName())
                    .value("created", clientCreds.getCreated())
                    .value("uri", clientCreds.getUri())
                    .value("descr", clientCreds.getDescr())
                    .value("type", clientCreds.getType())
                    .value("status", clientCreds.getStatus())
                    .value("details", clientCreds.getApplicationDetails())
                    ;
            session.execute(stmt);
        } catch(Throwable e) {
            log.error(e.getMessage(), e);
        }
    }


    @Override
    public boolean updateClientApp(String clientId, String scope, String description, Integer status, Map<String, String> applicationDetails) {
        // update CLIENTS_TABLE_NAME using clientId as key
        try {
            Update update = QueryBuilder.update(KEYSPACE_NAME, CLIENTS_TABLE_NAME);
            Update.Assignments assignments = update.with();
            if (scope != null && scope.length() > 0) {
                assignments.and(QueryBuilder.set("scope", scope));
            }
            if (description != null && description.length() > 0) {
                assignments.and(QueryBuilder.set("descr", description));
            }
            if (status != null) {
                assignments.and(QueryBuilder.set("status", status));
            }
            if (applicationDetails != null && applicationDetails.size() > 0) {
                assignments.and(QueryBuilder.set("details", applicationDetails));
            }
            Update.Where stmt = assignments.where(QueryBuilder.eq("client_id", clientId));

            session.execute(stmt);
            return true;
        } catch(Throwable e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }



    @Override
    public List<ApplicationInfo> getAllApplications() {
        // CLIENTS_TABLE_NAME
        List<ApplicationInfo> list = new ArrayList<ApplicationInfo>();
        Select stmt = QueryBuilder.select().from(KEYSPACE_NAME, CLIENTS_TABLE_NAME);
        ResultSet rs = session.execute(stmt);
        for (Row row : rs) {
            ApplicationInfo app = new ApplicationInfo();
            app.setId(row.getString("client_id"));
            app.setSecret(row.getString("client_secret"));
            app.setScope(row.getString("scope"));
            app.setName(row.getString("name"));
            app.setRegistered(row.getTimestamp("created"));
            app.setRedirectUri(row.getString("uri"));
            app.setDescription(row.getString("descr"));
//            app.set(row.getInt("type"));
            app.setStatus(row.getInt("status"));
            app.setApplicationDetails(row.getMap("details", String.class, String.class));
            list.add(app);
        }
        return list;
    }

}
