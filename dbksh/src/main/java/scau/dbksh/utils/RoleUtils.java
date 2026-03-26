package scau.dbksh.utils;

public final class RoleUtils {

    public static final String USER_ROLE = "\u7528\u6237\u7aef";
    public static final String ADMIN_ROLE = "\u7ba1\u7406\u7aef";
    private static final String LEGACY_ADMIN_ROLE = "\u7ba1\u7406\u5458";

    private RoleUtils() {
    }

    public static boolean isUserRole(String role) {
        return USER_ROLE.equals(role);
    }

    public static boolean isAdminRole(String role) {
        return ADMIN_ROLE.equals(role) || LEGACY_ADMIN_ROLE.equals(role);
    }

    public static String normalizeRole(String role) {
        if (isAdminRole(role)) {
            return ADMIN_ROLE;
        }
        if (isUserRole(role)) {
            return USER_ROLE;
        }
        return role;
    }
}
