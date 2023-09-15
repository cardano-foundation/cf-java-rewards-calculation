package org.cardanofoundation.rewards.enums;

public enum DataType {
    ADA_POTS ("adaPots"),
    EPOCH_INFO ("epochInfo"),
    PROTOCOL_PARAMETERS ("protocolParameters");

    public final String resourceFolderName;

    DataType(String resourceFolderName) {
        this.resourceFolderName = resourceFolderName;
    }
}
