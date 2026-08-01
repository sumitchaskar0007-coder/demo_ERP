package com.jadhavr.erp.admission.service;

public class UploadScanPendingException extends RuntimeException {
    public UploadScanPendingException() {
        super("Uploaded document security scan is still pending");
    }
}
