package service;

import model.TagsModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.Tag;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;

@Service
public class VideoService {
    public static final String VIDEO_CONTENT = "video/";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";

    public void putVideo(byte[] bytes, String bucketName, String fileName, String description) {
        try {
            String theTags = "name=" + fileName + "&description=" + description;

            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .tagging(theTags)
                    .build();

            S3Service.getInstance().putObject(putOb, RequestBody.fromBytes(bytes));
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    public String getTags(String bucketName){
        try {
            ListObjectsRequest listObjects = ListObjectsRequest
                    .builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsResponse res = S3Service.getInstance().listObjects(listObjects);
            List<S3Object> objects = res.contents();
            List<String> keys = new ArrayList<>();

            for (S3Object myValue: objects) {
                String key = myValue.key();

                GetObjectTaggingRequest getTaggingRequest = GetObjectTaggingRequest
                        .builder()
                        .key(key)
                        .bucket(bucketName)
                        .build();

                GetObjectTaggingResponse tags = S3Service.getInstance().getObjectTagging(getTaggingRequest);
                List<Tag> tagSet= tags.tagSet();
                for (Tag tag : tagSet)
                    keys.add(tag.value());
            }

            List<TagsModel> tagList = modList(keys);
            return convertToString(toXml(tagList));

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return "";
    }

    private List<TagsModel> modList(List<String> myList){
        int count = myList.size();
        List<TagsModel> allTags = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();

        for(int index = 0; index < count; index++) {
            if (index % 2 == 0)
                keys.add(myList.get(index));
            else
                values.add(myList.get(index));
        }

        TagsModel myTag;
        for (int i = 0; i < keys.size(); i++){
            myTag = new TagsModel();
            myTag.setName(keys.get(i));
            myTag.setDesc(values.get(i));
            allTags.add(myTag);
        }
        return allTags;
    }

    public ResponseEntity<byte[]> getObjectBytes(String bucketName, String keyName) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(keyName)
                    .bucket(bucketName)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = S3Service.getInstance().getObjectAsBytes(objectRequest);
            return ResponseEntity.status(HttpStatus.OK)
                    .header(CONTENT_TYPE, VIDEO_CONTENT + "mp4")
                    .header(CONTENT_LENGTH, String.valueOf(objectBytes.asByteArray().length))
                    .body(objectBytes.asByteArray());

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return null;
    }

    private Document toXml(List<TagsModel> itemList) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement( "Tags" );
            doc.appendChild(root);

            for (TagsModel myItem: itemList) {
                Element item = doc.createElement( "Tag" );
                root.appendChild(item);

                Element id = doc.createElement( "Name" );
                id.appendChild( doc.createTextNode(myItem.getName() ) );
                item.appendChild(id);

                Element name = doc.createElement( "Description" );
                name.appendChild(doc.createTextNode(myItem.getDesc()));
                item.appendChild(name);
            }

            return doc;
        } catch(ParserConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String convertToString(Document xml) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StreamResult result = new StreamResult(new StringWriter());

            DOMSource source = new DOMSource(xml);
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch(TransformerException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
