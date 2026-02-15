package com.lld.urlshortener.enums;

public enum UrlStatus {
    ACTIVE,      // URL is currently valid and can be redirected
    EXPIRED,     // URL has passed its expiration time
    DELETED      // URL was manually deleted by user
}
