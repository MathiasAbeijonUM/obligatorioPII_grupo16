package uy.edu.um.doors;

public class User {

    public enum UserType{
        ADMIN, GENERIC
    }

    private int uid;
    private String alias;
    private UserType type;

    public User(int uid, String alias, UserType type){
        this.uid = uid;
        this.alias = alias;
        this.type = type;
    }

    public int getUid(){
        return this.uid;
    }
    public String getAlias(){
        return this.alias;
    }
    public UserType getType(){
        return this.type;
    }

    @Override
    public String toString(){
        return "USER:" + alias + " UID:" + uid;
    }

}
