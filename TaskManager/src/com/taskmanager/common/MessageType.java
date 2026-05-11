package com.taskmanager.common;

public class MessageType {
    // 客户端 → 服务器
    public static final String LOGIN          = "LOGIN";
    public static final String REGISTER       = "REGISTER";
    public static final String GET_ALL_TASKS  = "GET_ALL_TASKS";
    public static final String GET_MY_TASKS   = "GET_MY_TASKS";
    public static final String CREATE_TASK    = "CREATE_TASK";
    public static final String UPDATE_STATUS  = "UPDATE_STATUS";
    public static final String UPDATE_ASSIGNEE= "UPDATE_ASSIGNEE";
    public static final String GET_HISTORY    = "GET_HISTORY";
    public static final String GET_ALL_USERS  = "GET_ALL_USERS";
    public static final String LOGOUT         = "LOGOUT";

    // 服务器 → 客户端
    public static final String SUCCESS        = "SUCCESS";
    public static final String ERROR          = "ERROR";
    public static final String TASK_LIST      = "TASK_LIST";
    public static final String USER_LIST      = "USER_LIST";
    public static final String HISTORY_LIST   = "HISTORY_LIST";
    public static final String LOGIN_SUCCESS  = "LOGIN_SUCCESS";
}