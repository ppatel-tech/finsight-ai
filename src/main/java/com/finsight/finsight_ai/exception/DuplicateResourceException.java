package com.finsight.finsight_ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException{
    public DuplicateResourceException(String message){
        super(message);
    }
}

//Why extend RuntimeException not Exception?
//Checked exceptions (Exception) force every caller to
//declare throws or wrap in try-catch. That creates noise
//throughout your codebase. Spring handles
//unchecked exceptions (RuntimeException) cleanly through the global handler.
//This is the standard pattern in Spring applications.