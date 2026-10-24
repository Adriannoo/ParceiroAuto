package br.edu.uniamerica.parceiro_auto.controller.dto.mapper;

import br.edu.uniamerica.parceiro_auto.controller.dto.user.UserResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.User;

public final class UserMapper {
    // Classe utilitaria: a conversao e feita pelos metodos estaticos.
    private UserMapper() {}

    public static UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(user.getId(), user.getLogin(), user.getName());
    }
}
