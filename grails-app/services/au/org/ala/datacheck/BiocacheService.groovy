package au.org.ala.datacheck

import grails.converters.JSON
import groovy.json.JsonOutput
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.DeleteMethod
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity

class BiocacheService {

    static transactional = false

    def grailsApplication
    def grailsLinkGenerator

    def authService

    def areColumnHeaders(String[] columnHeadersUnparsed){
      def post = new PostMethod(grailsApplication.config.biocacheService.baseURL + "/parser/areDwcTerms")
      def http = new HttpClient()
      JsonOutput jsonOutput = new JsonOutput()
      def json = jsonOutput.toJson(columnHeadersUnparsed)
      log.debug("[areColumnHeaders] Sending: " + json)
      //   post.addRequestHeader("Content-Type", "application/json; charset=UTF-8")
      post.setRequestBody(json)
      http.executeMethod(post)
      Boolean.parseBoolean(post.getResponseBodyAsString())
    }

    def guessColumnHeaders(String[] columnHeadersUnparsed){
      def post = new PostMethod(grailsApplication.config.biocacheService.baseURL + "/parser/matchTerms")
      def http = new HttpClient()
      JsonOutput jsonOutput = new JsonOutput()
      def json = jsonOutput.toJson(columnHeadersUnparsed)
      //   post.addRequestHeader("Content-Type", "application/json; charset=UTF-8")
      post.setRequestBody(json)
      http.executeMethod(post)
      def parseResponse = post.getResponseBodyAsString()  
      log.debug("Match terms response: " + parseResponse)
      JSON.parse(parseResponse)
    }

    def mapColumnHeaders(String[] columnHeadersUnparsed){
      def post = new PostMethod(grailsApplication.config.biocacheService.baseURL + "/parser/mapTerms")
      def http = new HttpClient()
      JsonOutput jsonOutput = new JsonOutput()
      def json = jsonOutput.toJson(columnHeadersUnparsed)      
      log.debug("###### column headers to map : "  + json)
      //  post.addRequestHeader("Content-Type", "application/json; charset=UTF-8")
      //post.setRequestBody(json)
      post.setRequestEntity( new StringRequestEntity( json ) );
      http.executeMethod(post)
      def resp =  post.getResponseBodyAsString()
      def map = JSON.parse(resp)
      def orderedMap = new LinkedHashMap<String,String>()
      columnHeadersUnparsed.each { key ->
           orderedMap.put(key, map.get(key))
      }
      orderedMap
    }

    def processRecord(String[] headers, String[] record){

      def post = new PostMethod(grailsApplication.config.biocacheService.baseURL + "/process/adhoc")
      def http = new HttpClient()
      //construct the map
      def map = new LinkedHashMap()
      headers.eachWithIndex { header, idx ->
          if(idx < record.length) {
              map.put(header, record[idx])
          }
      }
      JsonOutput jsonOutput = new JsonOutput()
      def requestAsJSON = jsonOutput.toJson(map)      
      log.debug(requestAsJSON)
      // post.addRequestHeader("Content-Type", "application/json; charset=UTF-8")
      post.setRequestBody(requestAsJSON)
      http.executeMethod(post)

      //parse the result as a ParsedRecord
      def responseAsJSON = post.getResponseBodyAsString()
      log.debug(responseAsJSON)
      def json = JSON.parse(responseAsJSON)

      ParsedRecord parsedRecord = new ParsedRecord()

      List<ProcessedValue> processedValues = new ArrayList<ProcessedValue>()
      List<QualityAssertion> qualityAssertions = new ArrayList<QualityAssertion>()

      json?.values?.each { obj ->
        if(!obj.name.endsWith("ID") && obj.name != "left" && obj.name != "right"){
            ProcessedValue processedValue = new ProcessedValue()
            processedValue.name = obj.name
            processedValue.raw = obj.raw
            processedValue.processed = obj.processed
            processedValues.add(processedValue)
        }
      }

      json?.assertions?.each { obj ->
        QualityAssertion qa = new QualityAssertion()
        qa.code = obj.code
        qa.comment = obj.comment
        qa.name = obj.name
        qa.qaStatus = obj.qaStatus
        qualityAssertions.add(qa)
      }

      parsedRecord.values = processedValues.toArray()
      parsedRecord.assertions = qualityAssertions.toArray()

      parsedRecord
    }

    /**
     * Upload the data to the biocache, passing back the response
     *
     * @param csvData
     * @param headers
     * @param datasetName
     * @param separator
     * @param firstLineIsData
     * @return response as string
     */
    def uploadData(String csvData, String headers, String datasetName, String separator,
                   String firstLineIsData, String customIndexedFields, String dataResourceUid){

      //post.setRequestBody(([csvData:csvData, headers:headers]) as JSON)
      List nameValuePairs = [
              new NameValuePair("csvData", csvData),
              new NameValuePair("headers", headers),
              new NameValuePair("datasetName", datasetName),
              new NameValuePair("separator", separator),
              new NameValuePair("firstLineIsData", firstLineIsData),
              new NameValuePair("customIndexedFields", customIndexedFields),
              new NameValuePair("uiUrl", grailsApplication.config.biocache.baseURL),
              new NameValuePair("alaId", authService.getUserId())
      ]

      //add the data resource UID if supplied
      if(dataResourceUid){
        nameValuePairs << new NameValuePair("dataResourceUid", dataResourceUid)
      }

        def post = new PostMethod(grailsApplication.config.biocacheService.baseURL + "/upload/post")
      post.setRequestBody(nameValuePairs.toArray(new NameValuePair[0]))

      def http = new HttpClient()
      http.executeMethod(post)

      //TODO check the response
      def uploadResp = post.getResponseBodyAsString()
      log.debug(uploadResp)

      //reference the UID caches
      def get = new GetMethod(grailsApplication.config.biocacheService.baseURL + "/cache/refresh")
      http.executeMethod(get)

      uploadResp
    }

    /**
     * Upload the data to the biocache, passing back the response
     *
     * @param csvData
     * @param headers
     * @param datasetName
     * @param separator
     * @param firstLineIsData
     * @return response as string
     */
    def uploadFile(String fileId, String headers, String datasetName, String separator,
                   String firstLineIsData, String customIndexedFields, String dataResourceUid){

      def csvUrl = grailsLinkGenerator.link(controller: 'dataCheck', action: 'serveFile', params: [fileId: fileId], absolute: true)
      log.debug("Using $csvUrl for biocache upload service csvZippedUrl")

      List nameValuePairs = [
              new NameValuePair("csvZippedUrl", csvUrl),
              new NameValuePair("headers", headers),
              new NameValuePair("datasetName", datasetName),
              new NameValuePair("separator", separator),
              new NameValuePair("firstLineIsData", firstLineIsData),
              new NameValuePair("customIndexedFields", customIndexedFields),
              new NameValuePair("uiUrl", grailsApplication.config.biocache.baseURL),
              new NameValuePair("alaId", authService.getUserId())
      ]

      //add the data resource UID if supplied
      if(dataResourceUid){
          nameValuePairs << new NameValuePair("dataResourceUid", dataResourceUid)
      }

      def post = new PostMethod(grailsApplication.config.biocacheService.baseURL + "/upload/")
      post.setRequestBody(nameValuePairs.toArray(new NameValuePair[0]))

      def http = new HttpClient()
      http.executeMethod(post)

      def uploadResp = post.getResponseBodyAsString()
      log.debug("Uploading response:" + uploadResp)

      //reference the UID caches
      def get = new GetMethod(grailsApplication.config.biocacheService.baseURL + "/cache/refresh")
      http.executeMethod(get)

      uploadResp
    }

    def uploadStatus(String uid){
      def http = new HttpClient()
        def get = new GetMethod(grailsApplication.config.biocacheService.baseURL + "/upload/status/${uid}")
      http.executeMethod(get)
      get.getResponseBodyAsString()
    }

    Boolean deleteResource(String uid){
        def http = new HttpClient()
        def delete = new DeleteMethod(grailsApplication.config.biocacheService.baseURL + "/upload/${uid}?apiKey=" + grailsApplication.config.apiKey)
        def responseCode = http.executeMethod(delete)
        responseCode == 200
    }

    def getCustomIndexes(String uid){
        def http = new HttpClient()
        def get = new GetMethod(grailsApplication.config.biocacheService.baseURL + "/upload/customIndexes/${uid}.json")
        http.executeMethod(get)
        JSON.parse(get.getResponseBodyAsString())
    }

    def saveChartOptions(String uid, options){
        def http = new HttpClient()
        def post = new PostMethod(grailsApplication.config.biocacheService.baseURL + "/upload/charts/${uid}")
        post.setRequestBody((options as JSON).toString())
        int status = http.executeMethod(post)
        status
    }

    def getChartOptions(String uid){
        def http = new HttpClient()
        def get = new GetMethod(grailsApplication.config.biocacheService.baseURL + "/upload/charts/${uid}")
        http.executeMethod(get)
        JSON.parse(get.getResponseBodyAsString())
    }

    def saveLayerOptions(String uid, options){
        def http = new HttpClient()
        def post = new PostMethod(grailsApplication.config.biocacheService.baseURL + "/upload/layers/${uid}")
        post.setRequestBody((options as JSON).toString())
        int status = http.executeMethod(post)
        status
    }

    def getLayerOptions(String uid){
        def http = new HttpClient()
        def get = new GetMethod(grailsApplication.config.biocacheService.baseURL + "/upload/layers/${uid}")
        http.executeMethod(get)
        JSON.parse(get.getResponseBodyAsString())
    }
}