package cc.blynk.integration.https;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.web.Organization;
import cc.blynk.server.core.model.web.Role;
import cc.blynk.server.core.model.web.UserInvite;
import cc.blynk.utils.JsonParser;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class OrganizationAPITest extends APIBaseTest {

    @Test
    public void getOrgNotAuthorized() throws Exception {
        HttpGet getOwnProfile = new HttpGet(httpsAdminServerUrl + "/organization/1");
        try (CloseableHttpResponse response = httpclient.execute(getOwnProfile)) {
            assertEquals(401, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getOrganizationWithRegularUser() throws Exception {
        login(regularUser.email, regularUser.pass);

        HttpGet req = new HttpGet(httpsAdminServerUrl + "/organization/1");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
            assertEquals("BLynk Inc.", fromApi.name);
            assertEquals("Europe/Kiev", fromApi.tzName);
        }
    }

    @Test
    public void getOrganization() throws Exception {
        login(admin.email, admin.pass);

        HttpGet req = new HttpGet(httpsAdminServerUrl + "/organization/1");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
            assertEquals("BLynk Inc.", fromApi.name);
            assertEquals("Europe/Kiev", fromApi.tzName);
        }
    }

    @Test
    public void updateOrganizationNotAllowedForRegularUser() throws Exception {
        login(regularUser.email, regularUser.pass);

        Organization organization = new Organization("1", "2", "/static/logo.png");

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createOrganizationNotAllowedForRegularAdmin() throws Exception {
        login(regularAdmin.email, regularAdmin.pass);

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png");

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void deleteOrganizationNotAllowedForRegularAdmin() throws Exception {
        holder.organizationDao.add(new Organization("BLynk Inc.", "Europe/Kiev", "/static/logo.png"));

        login(regularAdmin.email, regularAdmin.pass);

        HttpDelete req2 = new HttpDelete(httpsAdminServerUrl + "/organization/2");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createOrganization() throws Exception {
        login(admin.email, admin.pass);

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png");

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
        }
    }

    @Test
    public void updateOrganization() throws Exception {
        login(admin.email, admin.pass);

        Organization organization = new Organization("1", "2", "/static/logo.png");

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
        }
    }

    @Test
    public void deleteOrganization() throws Exception {
        createOrganization();

        HttpDelete req = new HttpDelete(httpsAdminServerUrl + "/organization/1");

        //do not allow to delete initial org
        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        HttpDelete req2 = new HttpDelete(httpsAdminServerUrl + "/organization/2");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void regularAdminCantDeleteOtherOrganization() throws Exception {
        createOrganization();

        //we don't need cookie from initial login here
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(initUnsecuredSSLContext(), new MyHostVerifier());
        CloseableHttpClient newHttpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        login(newHttpClient,  httpsAdminServerUrl, regularAdmin.email, regularAdmin.pass);

        HttpDelete req2 = new HttpDelete(httpsAdminServerUrl + "/organization/2");

        try (CloseableHttpResponse response = newHttpClient.execute(req2)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getUsersFromOrg() throws Exception {
        login(regularAdmin.email, regularAdmin.pass);

        HttpGet req = new HttpGet(httpsAdminServerUrl + "/organization/1/users");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            User[] fromApi = JsonParser.mapper.readValue(consumeText(response), User[].class);
            assertNotNull(fromApi);
            assertEquals(2, fromApi.length);
            for (User user : fromApi) {
                assertNotNull(user);
                assertNull(user.pass);
                assertNotEquals(regularAdmin.email, user.email);
            }
        }
    }

    @Test
    public void deleteRegularUserFromOrg() throws Exception {
        login(regularAdmin.email, regularAdmin.pass);


        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/delete");
        String body = JsonParser.mapper.writeValueAsString(new String[]{"user@blynk.cc"});
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet req2 = new HttpGet(httpsAdminServerUrl + "/organization/1/users");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            User[] fromApi = JsonParser.mapper.readValue(consumeText(response), User[].class);
            assertNotNull(fromApi);
            assertEquals(1, fromApi.length);
            for (User user : fromApi) {
                assertNotEquals("user@blynk.cc", user.email);
                assertNotEquals(regularAdmin.email, user.email);
            }
        }
    }

    @Test
    public void cantDeleteSuperAdmin() throws Exception {
        login(admin.email, admin.pass);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/delete");
        String body = JsonParser.mapper.writeValueAsString(new String[]{admin.email});
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void regularUserCantDelete() throws Exception {
        login(regularUser.email, regularUser.pass);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/delete");
        String body = JsonParser.mapper.writeValueAsString(new String[]{"user@blynk.cc"});
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

    }

    @Test
    public void updateAccountRole() throws Exception {
        login(admin.email, admin.pass);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/update");
        String body = JsonParser.mapper.writeValueAsString(new UserInvite("user@blynk.cc", "123", Role.ADMIN));
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));


        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet req2 = new HttpGet(httpsAdminServerUrl + "/organization/1/users");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            User[] fromApi = JsonParser.mapper.readValue(consumeText(response), User[].class);
            assertNotNull(fromApi);
            assertEquals(3, fromApi.length);
            for (User user : fromApi) {
                if (user.email.equals("user@blynk.cc")) {
                    assertEquals(Role.ADMIN, user.role);
                }
            }
        }
    }

    @Test
    public void updateAccountRoleForNonExistingUser() throws Exception {
        login(admin.email, admin.pass);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/update");
        String body = JsonParser.mapper.writeValueAsString(new UserInvite("userzzz@blynk.cc", "123", Role.ADMIN));
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));


        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

}
