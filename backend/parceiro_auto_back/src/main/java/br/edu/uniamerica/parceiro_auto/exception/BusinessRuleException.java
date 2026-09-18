package br.edu.uniamerica.parceiro_auto.exception;

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String mensagem) {
        super(mensagem);
    }
}
