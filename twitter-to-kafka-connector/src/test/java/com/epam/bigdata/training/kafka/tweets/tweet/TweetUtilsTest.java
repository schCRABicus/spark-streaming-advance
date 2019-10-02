package com.epam.bigdata.training.kafka.tweets.tweet;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Place;
import twitter4j.RateLimitStatus;
import twitter4j.Scopes;
import twitter4j.Status;
import twitter4j.SymbolEntity;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

import java.util.Date;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class TweetUtilsTest {

    @Test
    public void usernameOrDefaultWhenNoUserInStatus() {
        // given
        Status status = Mockito.mock(Status.class);
        BDDMockito.when(status.getUser()).thenReturn(null);

        // when
        String result = TweetUtils.usernameOrDefault(status);

        // then
        Assert.assertThat(result, Is.is("unknown"));
    }

    @Test
    public void usernameOrDefaultWhenUserWithoutName() {
        // given
        Status status = Mockito.mock(Status.class);
        User user = Mockito.mock(User.class);
        BDDMockito.when(status.getUser()).thenReturn(user);
        BDDMockito.when(user.getName()).thenReturn(null);

        // when
        String result = TweetUtils.usernameOrDefault(status);

        // then
        Assert.assertThat(result, Is.is("unknown"));
    }

    @Test
    public void usernameOrDefaultWhenNonEmptyUsername() {
        // given
        Status status = Mockito.mock(Status.class);
        User user = Mockito.mock(User.class);
        String name = "test user";
        BDDMockito.when(status.getUser()).thenReturn(user);
        BDDMockito.when(user.getName()).thenReturn(name);

        // when
        String result = TweetUtils.usernameOrDefault(status);

        // then
        Assert.assertThat(result, Is.is(name));
    }
}