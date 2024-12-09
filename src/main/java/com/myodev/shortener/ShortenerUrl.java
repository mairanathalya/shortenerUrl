package com.myodev.shortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShortenerUrl implements RequestHandler<Map<String,Object>, Map<String, String>> {
    //implementação de uma interface na classe para o metedo handlerequest para ajudar o metodo saber os parametros que vai receber, com isso identificar e extrair as informações
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final S3Client s3Client = S3Client.builder().build();
    @Override
    public Map<String, String> handleRequest(Map<String, Object> input, Context context) {
        //toda informação que foi passado na lambda vai ser repassado no objeto input, então consegue pegar o body da requisição
        String body = (String) input.get("body");

        //partindo para obter os dados do body
        //fazendo um try catch pq pode ser que os campos venha ou não, ou um possivel dados errados

        Map<String, String> bodyMap;

        try{
            bodyMap = objectMapper.readValue(body, Map.class);
        } catch (Exception exception){
            throw new RuntimeException("Error parsing JSON body: " + exception.getMessage(), exception);
        }

        //extrair os campos do body
        String originalUrl = bodyMap.get("originalUrl");
        String expirationTime = bodyMap.get("expirationTime");

        //identificar cada url encurtada e nomear cada arquivo json no s3
        String shortUrlCode = UUID.randomUUID().toString().substring(0,8);

        Long expirationTimeInSeconds = Long.parseLong(expirationTime);
        //empacotar as informações da originalUrl e expirationTime para um objeto json, que vai salvar no s3
        UrlData urlData = new UrlData(originalUrl, expirationTimeInSeconds);

        try{
            //transformando o ulrdata(objeto) em um json
           String urlDataJson = objectMapper.writeValueAsString(urlData);
           //novo request de criação de um novo objeto
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket("storage-shortener-myodev-lambda")
                    .key(shortUrlCode + ".json")
                    .build();
            //request para o s3 passando o conteudo do arquivo
            s3Client.putObject(request, RequestBody.fromString(urlDataJson));
        } catch (Exception exception){
            throw new RuntimeException("Error saving data to s3: " + exception.getMessage(), exception);
        }

        Map<String, String> response = new HashMap<>();
        response.put("code", shortUrlCode);

        return response;
    }
}