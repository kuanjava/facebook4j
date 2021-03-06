/*
 * Copyright 2007 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package facebook4j.auth;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import facebook4j.Facebook;
import facebook4j.FacebookFactory;
import facebook4j.FacebookTestBase;
import facebook4j.conf.Configuration;
import facebook4j.conf.ConfigurationBuilder;
import facebook4j.internal.http.HttpClientWrapper;
import facebook4j.internal.http.HttpParameter;
import facebook4j.internal.http.HttpResponse;

/**
 * @author Yusuke Yamamoto - yusuke at mac.com
 * @author Ryuji Yamashita - roundrop at gmail.com
 */
public class OAuthTest extends FacebookTestBase {
    
    @Test
    public void deterministic() throws Exception {
        ArrayList list1 = new ArrayList();
        ArrayList list2 = new ArrayList();
        assertThat(list1, is(list2));
        Facebook Facebook1 = new FacebookFactory().getInstance();
        Facebook1.setOAuthAppId(appId, appSecret);
        Facebook Facebook2 = new FacebookFactory().getInstance();
        Facebook2.setOAuthAppId(appId, appSecret);
        assertThat(Facebook1, is(Facebook2));
    }

    @Test
    public void OAuth() throws Exception {
        ConfigurationBuilder build = new ConfigurationBuilder();
        String oAuthAccessToken = p.getProperty("real.oauth.accessToken");
        String oAuthAppId = p.getProperty("real.oauth.appId");
        String oAuthAppSecret = p.getProperty("real.oauth.appSecret");
        build.setOAuthAccessToken(oAuthAccessToken);
        build.setOAuthAppId(oAuthAppId);
        build.setOAuthAppSecret(oAuthAppSecret);
        OAuthAuthorization auth = new OAuthAuthorization(build.build());
        Facebook fb = new FacebookFactory().getInstance(auth);
        fb.getId();
    }

    @Test(expected = IllegalStateException.class)
    public void illegalStatus() throws Exception {
        new FacebookFactory().getInstance().getOAuthAccessToken();
        fail("should throw IllegalStateException since AppID hasn't been acquired.");
    }

    @Test
    public void accessToken() throws Exception {
        ConfigurationBuilder build = new ConfigurationBuilder();
        build.setOAuthAppId(appId);
        build.setOAuthAppSecret(appSecret);
        Configuration configuration = build.build();
        HttpClientWrapper http = new HttpClientWrapper(configuration);
        HttpResponse res = http.get(configuration.getOAuthAccessTokenURL() +
                                    "?client_id=" + appId +
                                    "&client_secret=" + appSecret +
                                    "&grant_type=client_credentials");
        AccessToken at = new AccessToken(res);
        assertThat(at.getToken(), is(notNullValue()));
        assertThat(at.getExpires(), is(nullValue()));

        at = new AccessToken("6377362-kW0YV1ymaqEUCSHP29ux169mDeA4kQfhEuqkdvHk", 123456789012345L);
        assertThat(at.getToken(), is("6377362-kW0YV1ymaqEUCSHP29ux169mDeA4kQfhEuqkdvHk"));
        assertThat(at.getExpires(), is(123456789012345L));

        at = new AccessToken("6377362-kW0YV1ymaqEUCSHP29ux169mDeA4kQfhEuqkdvHk");
        assertThat(at.getToken(), is("6377362-kW0YV1ymaqEUCSHP29ux169mDeA4kQfhEuqkdvHk"));
        assertThat(at.getExpires(), is(nullValue()));

        at = new AccessToken("access_token=6377362-kW0YV1ymaqEUCSHP29ux169mDeA4kQfhEuqkdvHk&expires=123456789012345");
        assertThat(at.getToken(), is("6377362-kW0YV1ymaqEUCSHP29ux169mDeA4kQfhEuqkdvHk"));
        assertThat(at.getExpires(), is(123456789012345L));
    }

    @Test
    public void encodeParameter() throws Exception {
        //http://wiki.oauth.net/TestCases
        assertThat(HttpParameter.encode("abcABC123"), is("abcABC123"));
        assertThat(HttpParameter.encode("-._~"), is("-._~"));
        assertThat(HttpParameter.encode("%"), is("%25"));
        assertThat(HttpParameter.encode("+"), is("%2B"));
        assertThat(HttpParameter.encode("&=*"), is("%26%3D%2A"));
        assertThat(HttpParameter.encode("\n"), is("%0A"));
        assertThat(HttpParameter.encode("\u0020"), is("%20"));
        assertThat(HttpParameter.encode("\u007F"), is("%7F"));
        assertThat(HttpParameter.encode("\u0080"), is("%C2%80"));
        assertThat(HttpParameter.encode("\u3001"), is("%E3%80%81"));

        String unreserved = "abcdefghijklmnopqrstuvwzyxABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";
        assertThat(HttpParameter.encode(unreserved), is(unreserved));
    }

}
