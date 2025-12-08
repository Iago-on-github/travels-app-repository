package com.travel_system.backend_app.model.dtos.request;


import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record AdministratorRequestDTO(
        @Email(message = "Verifique a inserção do email")
        @NotNull
        @NotBlank(message = "Campo 'Email' é obrigatório")
        String email,
        @NotNull
        @Min(7)
        @NotBlank(message = "Campo 'Senha' é obrigatório")
        String password,
        @NotNull
        @Min(4)
        @NotBlank(message = "Campo 'Nome' é obrigatório")
        String name,
        String lastName,
        @NotNull
        @Min(8)
        @NotBlank(message = "Campo 'Telefone' é obrigatório")
        String telephone,
        String profilePicture
) {
}
