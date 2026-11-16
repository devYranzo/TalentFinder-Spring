package dev.enekoyranzo.talentfinder.document;

/** El archivo subido no es aceptable. El código es estable y va a la auditoría; el mensaje, al usuario. */
public class InvalidDocumentException extends RuntimeException {

    private final String code;

    public InvalidDocumentException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}