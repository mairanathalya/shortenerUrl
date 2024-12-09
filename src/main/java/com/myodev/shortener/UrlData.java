package com.myodev.shortener;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UrlData {
    //infos que representa a url original
    private String originalUrl;
    private Long expirationTime;

}
