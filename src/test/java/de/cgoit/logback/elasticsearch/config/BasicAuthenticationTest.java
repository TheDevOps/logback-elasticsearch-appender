package de.cgoit.logback.elasticsearch.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class BasicAuthenticationTest {

    private static final String ENV_VAR_SET_NAME = "ENV_VAR_SET";
    private static final String ENV_VAR_SET_KEY = "${env." + ENV_VAR_SET_NAME + "}";
    private static final String ENV_VAR_SET_VALUE = "ThisIsSet";
    private static final String ENV_VAR_NOT_SET_NAME = "NOT_SET";
    private static final String ENV_VAR_NOT_SET_KEY = "${env." + ENV_VAR_NOT_SET_NAME + "}";

    private MockedStatic<BasicAuthentication> mockedStatic;

    @Before
    public void setup() {
        mockedStatic = Mockito.mockStatic(BasicAuthentication.class);
        mockedStatic.when(() -> BasicAuthentication.getFromEnv(ArgumentMatchers.eq(ENV_VAR_SET_NAME))).thenReturn(ENV_VAR_SET_VALUE);
        mockedStatic.when(() -> BasicAuthentication.getFromEnv(ArgumentMatchers.eq(ENV_VAR_NOT_SET_NAME))).thenReturn(null);
    }

    @After
    public void tearDown() {
        mockedStatic.close();
    }

    @Test
    public void resolve_env_var_if_env_var_is_set() {
        BasicAuthentication auth = new BasicAuthentication("TheUsername", ENV_VAR_SET_KEY);

        mockedStatic.verify(() -> BasicAuthentication.getFromEnv(ENV_VAR_SET_NAME), times(1));
        assertEquals("Basic " + new String(Base64.getEncoder().encode(String.format("%s:%s", "TheUsername", ENV_VAR_SET_VALUE).getBytes())),
                getInternalState(auth, "authentication"));
    }

    @Test
    public void return_unresolved_env_var_if_env_var_is_not_set() {
        BasicAuthentication auth = new BasicAuthentication("TheUsername", ENV_VAR_NOT_SET_KEY);

        mockedStatic.verify(() -> BasicAuthentication.getFromEnv(ENV_VAR_NOT_SET_NAME), times(1));
        assertEquals("Basic " + new String(Base64.getEncoder().encode(String.format("%s:%s", "TheUsername", ENV_VAR_NOT_SET_KEY).getBytes())),
                getInternalState(auth, "authentication"));
    }

    @Test
    public void return_unresolved_if_no_env_var() {
        BasicAuthentication auth = new BasicAuthentication("TheUsername", "ThePassword");

        mockedStatic.verify(() -> BasicAuthentication.getFromEnv(any()), times(0));
        assertEquals("Basic " + new String(Base64.getEncoder().encode(String.format("%s:%s", "TheUsername", "ThePassword").getBytes())),
                getInternalState(auth, "authentication"));
    }

    private String getInternalState(BasicAuthentication auth, String fieldName) {
        try {
            Field field = BasicAuthentication.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(auth);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
