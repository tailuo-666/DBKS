package scau.dbksh.utils;

import scau.dbksh.dto.UserDTO;

public final class UserHolder {

    // 使用 ThreadLocal 保存单次请求内的登录用户，便于 service 直接读取当前用户信息。
    private static final ThreadLocal<UserDTO> USER_THREAD_LOCAL = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void saveUser(UserDTO user) {
        USER_THREAD_LOCAL.set(user);
    }

    public static UserDTO getUser() {
        return USER_THREAD_LOCAL.get();
    }

    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}
