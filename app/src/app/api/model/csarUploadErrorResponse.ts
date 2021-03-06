/**
 * TOSCAna
 * To be Done!
 *
 * OpenAPI spec version: 1.0.0-SNAPSHOT
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */
import { LogEntry } from './logEntry';


export interface CsarUploadErrorResponse {
    /**
     * The java class name of the exception that occurred on the server while performing the parsing operation.
     */
    exception: string;
    /**
     * The list of log entries that accumulated while performing the parsing of the CSAR
     */
    logs: Array<LogEntry>;
    /**
     * The messages of the exception
     */
    message: string;
    /**
     * The HTTP Path that was called and caused the exception.
     */
    path: string;
    /**
     * The HTTP Status code of the exception
     */
    status: number;
    /**
     * The Unix timestamp (in milliseconds) when this exception has occrued (Was constructed internally)
     */
    timestamp: number;
}
