package uk.gov.hmcts.reform.lib.config;

public interface TestConfig {

    String getIdamApiUrl();

    String getClientId();

    String getClientSecret();

    String getOauthRedirectUrl();

    String getScope();
}
