package br.edu.uniamerica.parceiro_auto.exception;

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String mensagem) {
        super(mensagem);
    }
}
