package uk.ac.westminster.smartcampusapi.model;

public class ErrorMessage {

    private String errorMessage;
    private int errorCode;
    private String documentation;

    // Default constructor - required by Jackson
    public ErrorMessage() {}

    public ErrorMessage(String errorMessage, int errorCode, String documentation) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.documentation = documentation;
    }

    public String getErrorMessage()        { return errorMessage; }
    public void setErrorMessage(String m)  { this.errorMessage = m; }

    public int getErrorCode()              { return errorCode; }
    public void setErrorCode(int c)        { this.errorCode = c; }

    public String getDocumentation()       { return documentation; }
    public void setDocumentation(String d) { this.documentation = d; }
}