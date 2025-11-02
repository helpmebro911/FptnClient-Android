package org.fptn.vpn.database.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.utils.CountryFlags;

@Entity(tableName = "server_table")
public class FptnServerDto {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public boolean isSelected;
    public String name;
    public String username;
    public String password;
    public String host;
    public int port;
    public String countryCode;
    public String md5ServerFingerprint;
    public boolean censured;

    public FptnServerDto(int id, boolean isSelected, String name, String username, String password, String host, int port, String countryCode, String md5ServerFingerprint, boolean censured) {
        this.id = id;
        this.isSelected = isSelected;
        this.name = name;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.countryCode = countryCode;
        this.md5ServerFingerprint = md5ServerFingerprint;
        this.censured = censured;
    }

    public String getServerInfo() {
        String flag = CountryFlags.getCountryFlagByCountryCode(countryCode);
        return name + " (" + (flag != null ? flag : host) + ")";
    }

    public static final FptnServerDto AUTO =
            new FptnServerDto(Constants.SELECTED_SERVER_ID_AUTO, false, "Auto", "Auto", "Auto", "", 0, null, null, false);
}
