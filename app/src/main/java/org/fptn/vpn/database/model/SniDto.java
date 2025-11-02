package org.fptn.vpn.database.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sni_table", indices = {@Index(value = {"sni"}, unique = true)})
public class SniDto {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String sni;

    public SniDto(String sni) {
        this.sni = sni;
    }
}
