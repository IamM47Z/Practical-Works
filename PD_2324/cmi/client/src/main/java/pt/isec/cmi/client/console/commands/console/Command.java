package pt.isec.cmi.client.console.commands.console;

import pt.isec.cmi.shared.requests.RequestType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command
{
    RequestType requestType() default RequestType.NONE;
    String name();
    String usage();
    String description();
}