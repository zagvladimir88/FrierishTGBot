package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.telegram.ChatAdminChecker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SettingsAccessTest {

    private final ChatAdminChecker adminChecker = mock(ChatAdminChecker.class);
    private final SettingsAccess access = new SettingsAccess(adminChecker);

    @Test
    void everyoneConfiguresPrivateChatAndAnonymousAdminsConfigureGroup() {
        assertThat(access.canConfigure(ChatContext.privateChat(1))).isTrue();
        assertThat(access.canConfigure(new ChatContext(-100, -100, true, true))).isTrue();
        verifyNoInteractions(adminChecker);
    }

    @Test
    void asksTelegramAboutGroupMembers() {
        when(adminChecker.isAdmin(-100, 5)).thenReturn(true);
        when(adminChecker.isAdmin(-100, 6)).thenReturn(false);

        assertThat(access.canConfigure(new ChatContext(-100, 5, true, false))).isTrue();
        assertThat(access.canConfigure(new ChatContext(-100, 6, true, false))).isFalse();
    }
}