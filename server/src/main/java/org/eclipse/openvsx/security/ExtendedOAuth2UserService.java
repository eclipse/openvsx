package org.eclipse.openvsx.security;

import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.UserDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@Service
public class ExtendedOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserDataRepository userRepository;
    @Autowired
    EntityManager entityManager;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oauth2UserRequest, OAuth2User oauth2User) {
        String registrationId = oauth2UserRequest.getClientRegistration().getRegistrationId();



        String loginName = "github".equals(registrationId) ? oauth2User.getAttribute("login") : oauth2User.getAttribute("github_handle");
        var userData = userRepository.findByProviderAndLoginName("github", loginName);
        if(userData != null) {
            userData = updateExistingUser(userData, oauth2User);
        } else {
            userData = registerNewUser(oauth2UserRequest, oauth2User);
        }

        return oauth2User;
    }


    private UserData registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2User oauth2User) {
        var user = new UserData();

        user.setProvider("github");
        user.setProviderId(oauth2User.getName());
        user.setLoginName(oauth2User.getAttribute("login"));
        user.setFullName(oauth2User.getAttribute("name"));
        user.setEmail(oauth2User.getAttribute("email"));
        user.setProviderUrl(oauth2User.getAttribute("html_url"));
        user.setAvatarUrl(oauth2User.getAttribute("avatar_url"));

        entityManager.persist(user);
        return user;
    }

    private UserData updateExistingUser(UserData existingUser, OAuth2User oauth2User) {
        // existingUser.setName(oauth2User.getName()); // TODO update user
        entityManager.persist(existingUser);
        return existingUser;
    }

}