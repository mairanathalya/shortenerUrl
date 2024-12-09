package com.myodev.shortener;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RedirectUrl implements RequestHandler<Map<String,Object>, Map<String, Object>> {
    //criar o client para acessar o s3
    private final S3Client s3Client = S3Client.builder().build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        //recuperando o uuid da url encurtada para poder acessar os dados da url encurtada no s3
        //aqui ta extraindo o uuid da url encurtada pela requisição
        String pathParameters = (String) input.get("rawPath");
        String shortUrlCode = pathParameters.replace("/", "");

        //validação se o shortUrlcode existe
        if (shortUrlCode.isEmpty()) {
            throw new IllegalArgumentException("Invalid input: 'shortUrlCode' is required.");
        }

        //precisa transformar o arquivo que está em stream de pacotes em arquivos
        InputStream s3ObjectStream;

        //recuperando os arquivos do bucket no s3
        try {
            //request de get no bucket do s3
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket("storage-shortener-myodev-lambda")
                    .key(shortUrlCode + ".json")
                    .build();

            s3ObjectStream = s3Client.getObject(request);
        } catch(Exception exception) {
            throw new RuntimeException("Error fetching data from s3: " + exception.getMessage(), exception);
        }

        //extrair o dado da url do s3ObjectStream
        UrlData urlData;

        //transformar a stream em um objeto baseado no objeto urldata
        try {
            urlData = objectMapper.readValue(s3ObjectStream, UrlData.class);
        } catch(Exception exception){
            throw new RuntimeException("Error deserializing data from s3: " + exception.getMessage(), exception);
        }

        //verificar se a url ta valida, se o expiration time foi atingido, caso não, está valido e redireciona
        long currentTimeInSeconds = System.currentTimeMillis() / 1000;

        Map<String, Object> response = new HashMap<>();

        if(currentTimeInSeconds > urlData.getExpirationTime()){
            //cenário onde a url expirou
            response.put("statusCode" , "410");
            response.put("body" , "This Url has expired");

            return response;
        }

        //mapeando a resposta
        //cenario onde a url é valida
        Map<String, String> headers = new HashMap<>();
        headers.put("Location", urlData.getOriginalUrl());

        response.put("statusCode" , "302");
        response.put("headers", headers);

        return response;
    }
}