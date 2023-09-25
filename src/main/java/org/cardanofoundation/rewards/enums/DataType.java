package org.cardanofoundation.rewards.enums;

public enum DataType {
    ADA_POTS ("adaPots"),
    EPOCH_INFO ("epochInfo"),
    PROTOCOL_PARAMETERS ("protocolParameters"),
    POOL_DEREGISTRATIONS ("poolDeregistrations"),
    POOL_PARAMETERS ("poolParameters"),
    POOL_HISTORY ("poolHistory"),
    ACCOUNT_UPDATES ("accountUpdates"),
    POOL_OWNER_HISTORY("accountHistory"),
    MIR_CERTIFICATE("mirCertificates");

    public final String resourceFolderName;

    DataType(String resourceFolderName) {
        this.resourceFolderName = resourceFolderName;
    }
}
