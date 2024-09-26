package com.f8.turnera.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.zone.ZoneRulesException;
import java.util.List;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.f8.turnera.domain.dtos.ResponseDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class ErrorHandler {

        // Esta excepcion es nativa de spring y respeta los tagueos de los DTOs
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ResponseDTO> methodArgumentNotValidException(HttpServletRequest request,
                        MethodArgumentNotValidException e) {
                // get spring errors
                BindingResult result = e.getBindingResult();
                List<FieldError> fieldErrors = result.getFieldErrors();

                // convert errors to standard string
                StringBuilder errorMessage = new StringBuilder();
                fieldErrors.forEach(f -> errorMessage.append(f.getDefaultMessage() + ". "));

                log.error(errorMessage.toString());
                // return error info object with standard json
                ResponseDTO response = new ResponseDTO(null, "Campos inválidos.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ResponseDTO> constraintViolationException(HttpServletRequest request,
                        ConstraintViolationException e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));

                // convert errors to standard string
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Message: ").append(e.getMessage());

                log.error(errorMessage.toString());
                // return error info object with standard json
                ResponseDTO response = new ResponseDTO(null, "Campos inválidos.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ResponseDTO> exception(HttpServletRequest request, Exception e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));

                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Message: ").append(e.getMessage()).append(" StackTrace: ")
                                .append(errors.toString());

                log.error(errorMessage.toString());

                ResponseDTO response = new ResponseDTO(null, "No se pudo realizar la operación. Intente más tarde.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ResponseDTO> illegalArgumentException(HttpServletRequest request,
                        IllegalArgumentException e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));

                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Message: ").append(e.getMessage()).append(" StackTrace: ")
                                .append(errors.toString());

                log.error(errorMessage.toString());

                ResponseDTO response = new ResponseDTO(null, "No se pudo realizar la operación. Intente más tarde.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // TODO: revisar porque esto también salta si quiero guardar algo no nulo, por
        // ej la pwd en crear usuario
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ResponseDTO> dataIntegrityViolationException(HttpServletRequest request,
                        DataIntegrityViolationException e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));

                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Message: ").append(e.getMessage()).append(" StackTrace: ")
                                .append(errors.toString());

                log.error(errorMessage.toString());

                ResponseDTO response = new ResponseDTO(null,
                                "No se puede eliminar una entidad con asociaciones. Elimine primero las asociaciones.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MessagingException.class)
        public ResponseEntity<ResponseDTO> messagingException(HttpServletRequest request, MessagingException e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));

                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Message: ").append(e.getMessage()).append(" StackTrace: ")
                                .append(errors.toString());

                log.error(errorMessage.toString());

                ResponseDTO response = new ResponseDTO(null, "No se pudo realizar la operación. Intente más tarde.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ResponseDTO> forbiddenException(HttpServletRequest request, ForbiddenException e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));

                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Message: ").append(e.getMessage()).append(" StackTrace: ")
                                .append(errors.toString());

                log.error(errorMessage.toString());

                ResponseDTO response = new ResponseDTO(null, "Acceso no permitido.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(NoContentException.class)
        public ResponseEntity<ResponseDTO> noContentException(HttpServletRequest request, NoContentException e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));

                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Message: ").append(e.getMessage());

                log.error(errorMessage.toString());

                ResponseDTO response = new ResponseDTO(null, "No se pudo realizar la operación. Intente más tarde.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(NoContentCustomException.class)
        public ResponseEntity<ResponseDTO> noContentCustomException(HttpServletRequest request,
                        NoContentCustomException e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));

                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Message: ").append(e.getMessage());

                log.error(errorMessage.toString());

                ResponseDTO response = new ResponseDTO(null, e.getMessage());
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ResponseDTO> badRequestException(HttpServletRequest request, BadRequestException e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));

                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Message: ").append(e.getMessage());

                log.error(errorMessage.toString());

                ResponseDTO response = new ResponseDTO(null, e.getMessage());
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ZoneRulesException.class)
        public ResponseEntity<ResponseDTO> zoneRulesException(HttpServletRequest request, ZoneRulesException e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));

                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Message: ").append(e.getMessage());

                log.error(errorMessage.toString());

                ResponseDTO response = new ResponseDTO(null,
                                "La zona horaria de su equipo no es válida. Por favor modifique esta.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

}
