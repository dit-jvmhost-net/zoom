package utils;

@FunctionalInterface
public interface Handler {

	abstract void handle() throws Exception; //NOSONAR

}
