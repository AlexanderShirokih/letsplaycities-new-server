package ru.quandastudio.lpsserver.models;

/**
 * Interface that used to combine {@link LPSClientMessage.LPSLogIn} and {@link ru.quandastudio.lpsserver.http.model.SignUpRequest}
 */
public interface ISignInMessage {
    /**
     * @return user login
     */
    String getLogin();

    /**
     * @return firebase token
     */
    String getFirebaseToken();

    /**
     * @return social network ID
     */
    String getSnUID();

    /**
     * @return account type
     */
    AuthType getAuthType();
}
