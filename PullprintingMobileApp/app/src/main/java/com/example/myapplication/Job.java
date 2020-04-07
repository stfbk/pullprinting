package com.example.myapplication;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Job {

    @SerializedName("ticketUrl")
    @Expose
    private String ticketUrl;
    @SerializedName("printerType")
    @Expose
    private String printerType;
    @SerializedName("printerName")
    @Expose
    private String printerName;
    @SerializedName("errorCode")
    @Expose
    private String errorCode;
    @SerializedName("updateTime")
    @Expose
    private String updateTime;
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("ownerId")
    @Expose
    private String ownerId;
    @SerializedName("tags")
    @Expose
    private String tags;
    @SerializedName("uiState")
    @Expose
    private String uiState;
    @SerializedName("numberOfPages")
    @Expose
    private String numberOfPages;
    @SerializedName("createTime")
    @Expose
    private String createTime;
    @SerializedName("semanticState")
    @Expose
    private String semanticState;
    @SerializedName("printerid")
    @Expose
    private String printerid;
    @SerializedName("fileUrl")
    @Expose
    private String fileUrl;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("rasterUrl")
    @Expose
    private String rasterUrl;
    @SerializedName("contentType")
    @Expose
    private String contentType;
    @SerializedName("status")
    @Expose
    private String status;

    public String getTicketUrl() {
        return ticketUrl;
    }

    public String getPrinterType() {
        return printerType;
    }

    public String getPrinterName() {
        return printerName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getTags() {
        return tags;
    }

    public String getUiState() {
        return uiState;
    }

    public String getNumberOfPages() {
        return numberOfPages;
    }

    public String getCreateTime() {
        return createTime;
    }

    public String getSemanticState() {
        return semanticState;
    }

    public String getPrinterid() {
        return printerid;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getId() {
        return id;
    }

    public String getRasterUrl() {
        return rasterUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStatus() {
        return status;
    }
}