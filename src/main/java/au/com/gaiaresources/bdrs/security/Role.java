package au.com.gaiaresources.bdrs.security;

import java.util.List;

import edu.emory.mathcs.backport.java.util.Arrays;


public class Role {
    public static final String ROOT = "ROLE_ROOT";
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String SUPERVISOR = "ROLE_SUPERVISOR";
    public static final String POWERUSER = "ROLE_POWER_USER";
    public static final String USER = "ROLE_USER";
    public static final String LIMITED_USER = "ROLE_LIMITED_USER";  // same as USER but cannot download.
    public static final String ANONYMOUS = "ROLE_BDRS_ANONYMOUS";

    private static final String[] ROLE_ORDER = { ROOT, ADMIN, SUPERVISOR,
            POWERUSER, USER, LIMITED_USER, ANONYMOUS };

    public static void checkRole(String text) throws SecurityException {
        for (String s : ROLE_ORDER) {
            if (s.equals(text)) {
                return;
            }
        }
        throw new SecurityException("String is not a valid role: " + text);
    }

    private static int getRoleOrderIndex(String role) {
        for (int i = 0; i < ROLE_ORDER.length; ++i) {
            if (ROLE_ORDER[i].equals(role)) {
                return i;
            }
        }
        throw new SecurityException("String is not a valid role: " + role);
    }

    public static final String[] getAllRoles() {
        String[] copy = new String[ROLE_ORDER.length];
        System.arraycopy(ROLE_ORDER, 0, copy, 0, ROLE_ORDER.length);
        return copy;
    }
    
    /**
     * Returns all of the roles as a single comma-separated String.
     * @return
     */
    public static final String getRolesString() {
        return getRolesString(Arrays.asList(getAllRoles()));
    }
    
    /**
     * Returns the roles in the list as a single comma-separated String.
     * @param roleList
     * @return
     */
    public static final String getRolesString(List<String> roleList) {
        StringBuilder roles = new StringBuilder();
        for (String role : roleList)
        {
                roles.append(role);
                roles.append(",");
        }
        return roles.toString();
    }
    
    public static String[] getRolesLowerThan(String role) {
        int startIdx = getRoleOrderIndex(role) + 1;
        if (startIdx >= ROLE_ORDER.length) {
            return new String[0];
        }
        String[] result = new String[ROLE_ORDER.length - startIdx];
        System.arraycopy(ROLE_ORDER, startIdx, result, 0, ROLE_ORDER.length
                - startIdx);
        return result;
    }

    public static String[] getRolesLowerThanOrEqualTo(String role) {
        int startIdx = getRoleOrderIndex(role);
        if (startIdx >= ROLE_ORDER.length) {
            return new String[0];
        }
        String[] result = new String[ROLE_ORDER.length - startIdx];
        System.arraycopy(ROLE_ORDER, startIdx, result, 0, ROLE_ORDER.length
                - startIdx);
        return result;
    }

    public static String[] getRolesHigherThan(String role) {
        int endIdx = getRoleOrderIndex(role) - 1;
        if (endIdx < 0) {
            return new String[0];
        }
        String[] result = new String[endIdx + 1];
        System.arraycopy(ROLE_ORDER, 0, result, 0, endIdx + 1);
        return result;
    }

    public static String[] getRolesHigherThanOrEqualTo(String role) {
        int endIdx = getRoleOrderIndex(role);
        if (endIdx < 0) {
            return new String[0];
        }
        String[] result = new String[endIdx + 1];
        System.arraycopy(ROLE_ORDER, 0, result, 0, endIdx + 1);
        return result;
    }

    public static String getHighestRole(String[] roles) {
        if (roles == null || roles.length == 0) {
            return null;
        }
        for (int i = 0; i < ROLE_ORDER.length; ++i) {
            for (int j = 0; j < roles.length; ++j) {
                if (ROLE_ORDER[i].equals(roles[j])) {
                    return ROLE_ORDER[i];
                }
            }
        }
        return null;
    }

    public static String getLowestRole(String[] roles) {
        if (roles == null || roles.length == 0) {
            return null;
        }
        for (int i = ROLE_ORDER.length - 1; i >= 0; --i) {
            for (int j = 0; j < roles.length; ++j) {
                if (ROLE_ORDER[i].equals(roles[j])) {
                    return ROLE_ORDER[i];
                }
            }
        }
        return null;
    }
    
    public static Boolean isRoleHigherThanOrEqualTo(String expectedHigher, String expectedLower)
    {
        return getRoleOrderIndex(expectedHigher) <= getRoleOrderIndex(expectedLower);
    }
    
    public static Boolean isRoleHigher(String expectedHigher, String expectedLower)
    {
        return getRoleOrderIndex(expectedHigher) < getRoleOrderIndex(expectedLower);
    }
}
