package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Customer {
    private final String uid;
    private final String name;
    private AccountType accountType;
    private AccountStatus accountStatus;
    private Notes notes;
    private final Log log;
    private final String address;

    @JsonCreator
    public Customer(
            @JsonProperty("uid")
            String uid,
            @JsonProperty("name")
            String name,
            @JsonProperty("accountType")
            AccountType accountType,
            @JsonProperty("accountStatus")
            AccountStatus accountStatus,
            @JsonProperty("notes")
            Notes notes,
            @JsonProperty("log")
            Log log,
            @JsonProperty("address")
            String address) {
        this.uid = uid;
        this.name = name;
        this.notes = notes;
        this.log = log;
        this.accountType = accountType;
        this.accountStatus = accountStatus;
        this.address = address;
    }

    public void setAccountStatus(AccountStatus status) {
        this.accountStatus = status;
    }

    public String getName() {
        return name;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public Notes getNotes() {
        return notes;
    }

    public Log getLog() {
        return log;
    }

    public void setAccountType(AccountType type) {
        this.accountType = type;
    }

    public String getUid() {
        return uid;
    }

    public void setNotes(Notes notes) {
        this.notes = notes;
    }

    public String getAddress() {
        return address;
    }
}
