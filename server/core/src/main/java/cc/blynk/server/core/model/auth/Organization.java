package cc.blynk.server.core.model.auth;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.04.17.
 */
public class Organization {

    public static final Organization DEFAULT_ORGANIZATION = new Organization(0, "Blynk Inc.", "Europe/Kiev", null, 0);

    public int id;

    public String name;

    public String tzName;

    public String logoUrl;

    public int color;

    public Organization() {
    }

    public Organization(int id, String name, String tzName, String logoUrl, int color) {
        this.id = id;
        this.name = name;
        this.tzName = tzName;
        this.logoUrl = logoUrl;
        this.color = color;
    }
}
