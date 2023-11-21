package com.picpaysimplificado.service;

import com.picpaysimplificado.domain.Transaction.Transaction;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.dtos.TransactionDTO;
import com.picpaysimplificado.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository repository;

    //RestTemplate: CLASSE QUE O SPRING NOS OFERECE PARA FAZER COMUNICAÇÕES HTTP ENTRE SERVIÇOS
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NotificationService notificationService;

    @Value("${picpay.authorization.transaction}")
    private String authorizationUrl;

    public TransactionService() {
    }

    public Transaction createTransaction(TransactionDTO transaction) throws Exception {

        User sender = this.userService.findUserById(transaction.senderId());
        User receiver = this.userService.findUserById(transaction.receiverId());

        //VOU VERIFICAR SE ESSA TRANSAÇÃO SERÁ AUTORIZADA PELO PICPAY
        userService.validateTransaction(sender, transaction.value());

        boolean isAuthorized = this.authorizeTransaction();
        if(!isAuthorized) {
            throw new Exception("Transação não autorizada");
        }

        final var newTransaction = Transaction.builder()
                .amount(transaction.value())
                .sender(sender)
                .receiver(receiver)
                .timestamp(LocalDateTime.now())
                .build();

        sender.getBalance().subtract(transaction.value());
        receiver.getBalance().add(transaction.value());

        this.repository.save(newTransaction);
        this.userService.saveUser(sender);
        this.userService.saveUser(receiver);

        this.notificationService.sendNotification(sender, "Transação realizada com sucesso");
        this.notificationService.sendNotification(sender, "Transação recebida com sucesso");

        return newTransaction;
    }

    public boolean authorizeTransaction() {

        //RestTemplate: PARA FAZER REQUISIÇÕES DO TIPO GET, POST, PUT E ETC (TEM QUE FAZER A CONFIGURAÇÃO DELA)
        //PASSAMOS A URL POR HARD CODED, MAS O IDEAL É PASSAR COMO VARIAVEL DE AMBIENTE
        //ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity("https://run.mocky.io/v3/5794d450-d2e2-4412-8131-73d0293ac1cc", Map.class);
        ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity(authorizationUrl, Map.class);

        //VERFICA SE O STATUS É OK. SE FOR PEGA A KEY "MESSAGE" E VERIFICA SE O VALOR DELA É "AUTORIZADO"
        //ESSA MENSAGEM É VERIFICADA NO SERVIÇO DO PRÓPRIO PICPAY
        if(authorizationResponse.getStatusCode() == HttpStatus.OK) {
            String message = (String) authorizationResponse.getBody().get("message");
            return "Autorizado".equalsIgnoreCase(message);
        } else return false;
    }

    public List<Transaction> getAllTransactions() {
        return this.repository.findAll();
    }
}
