package com.fasterxml.jackson.dataformat.protobuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.*;

import junit.framework.TestCase;

abstract class ProtobufTestBase extends TestCase
{
    /*
    /**********************************************************
    /* Basic protoc definitions
    /**********************************************************
     */
    
    final protected static String PROTOC_SEARCH_REQUEST = "message SearchRequest {\n"
            +" required string query = 1;\n"
            +" optional int32 page_number = 2;\n"
            +" optional int32 result_per_page = 3;\n"
            +" enum Corpus {\n"
            +"   UNIVERSAL = 0;\n"
            +"   WEB = 1;\n"
            +" }\n"
            +" optional Corpus corpus = 4 [default = UNIVERSAL];\n"
            +"}\n"
    ;

    final protected static String PROTOC_POINT =
            "message Point {\n"
            +" required int32 x = 1;\n"
            +" required sint32 y = 2;\n"
            +"}\n";

    final protected static String PROTOC_BOX =
            PROTOC_POINT
            +"message Box {\n"
            +" required Point topLeft = 3;\n"
            +" required Point bottomRight = 5;\n"
            +"}\n"
    ;

    final protected static String PROTOC_NODE =
            "message Node {\n"
            +" required int32 id = 1;\n"
            +" optional Node left = 2;\n"
            +" optional Node right = 3;\n"
            +"}\n"
    ;

    final protected static String PROTOC_NAME =
            "message Name {\n"
            +" required string first = 2;\n"
            +" required string last = 7;\n"
            +"}\n"
    ;
    
    // protoc definition from 'jvm-serializers' project:
    final protected static String PROTOC_MEDIA_ITEM =
"package serializers.protobuf.media;\n"+
"option java_package = \"serializers.protobuf.media\";\n"+
"option java_outer_classname = \"MediaContentHolder\";\n"+
"option optimize_for = SPEED;\n"+
"\n"+
"message MediaItem {\n"+
"  required Media media = 1;\n"+
"  repeated Image images = 2;\n"+
"}\n"+
"message Image {\n"+
"  required string uri = 1;\n"+
"  optional string title = 2;\n"+
"  required int32 width = 3;\n"+
"  required int32 height = 4;\n"+
"  enum Size {\n"+
"    SMALL = 0;\n"+
"    LARGE = 1;\n"+
"  }\n"+
"  required Size size = 5;\n"+
"}\n"+
"message Media {\n"+
"  required string uri = 1;\n"+
"  optional string title = 2;\n"+
"  required int32 width = 3;\n"+
"  required int32 height = 4;\n"+
"  required string format = 5;\n"+
"  required int64 duration = 6;\n"+
"  required int64 size = 7;\n"+
"  optional int32 bitrate = 8;\n"+
"  repeated string persons = 9;\n"+
"  enum Player {\n"+
"    JAVA = 0;\n"+
"    FLASH = 1;\n"+
"}\n"+
"required Player player = 10;\n"+
"optional string copyright = 11;\n"+
"}\n"
;
    
    /*
    /**********************************************************
    /* POJO classes to use with protoc definitions
    /**********************************************************
     */

    static class Point {
        public int x;
        public int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Box {
        public Point topLeft, bottomRight;
        
        public Box(int x1, int y1, int x2, int y2) {
            topLeft = new Point(x1, y1);
            bottomRight = new Point(x2, y2);
        }
    }

    static class Name {
        public String first, last;

        public Name() { }
        public Name(String f, String l) {
            first = f;
            last = l;
        }
    }
    
    // // // POJOs for "JVM-serializers" case
    
    static class  MediaItem
    {
         public Media media;
         public List<Image> images;

         public MediaItem() { }
         
         public MediaItem addPhoto(Image i) {
             if (images == null) {
                 images = new ArrayList<Image>();
             }
             images.add(i);
             return this;
         }

         static MediaItem buildItem()
         {
             Media content = new Media();
             content.player = Player.JAVA;
             content.uri = "http://javaone.com/keynote.mpg";
             content.title = "Javaone Keynote";
             content.width = 640;
             content.height = 480;
             content.format = "video/mpeg4";
             content.duration = 18000000L;
             content.size = 58982400L;
             content.bitrate = 262144;
             content.copyright = "None";
             content.addPerson("Bill Gates");
             content.addPerson("Steve Jobs");

             MediaItem item = new MediaItem();
             item.media = content;

             item.addPhoto(new Image("http://javaone.com/keynote_large.jpg", "Javaone Keynote", 1024, 768, Size.LARGE));
             item.addPhoto(new Image("http://javaone.com/keynote_small.jpg", "Javaone Keynote", 320, 240, Size.SMALL));

             return item;
         }
    }

    enum Size { SMALL, LARGE };

    static class Image
    {
        public Image() { }
        public Image(String uri, String title, int w, int h, Size s) {
            this.uri = uri;
            this.title = title;
            width = w;
            height = h;
            size = s;
        }

        public String uri;
        public String title;
        public int width, height;
        public Size size;    
    } 

    enum Player { JAVA, FLASH; }

    static class Media {

        public String uri;
        public String title;        // Can be unset.
        public int width;
        public int height;
        public String format;
        public long duration;
        public long size;
        public int bitrate;         // Can be unset.

        public List<String> persons;
        
        public Player player;

        public String copyright;    // Can be unset.    

        public Media addPerson(String p) {
            if (persons == null) {
                persons = new ArrayList<String>();
            }
            persons.add(p);
            return this;
        }
    }
    
    /*
    /**********************************************************
    /* Additional assertion methods
    /**********************************************************
     */

    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }

    protected void assertToken(JsonToken expToken, JsonParser jp)
    {
        assertToken(expToken, jp.getCurrentToken());
    }

    protected void assertType(Object ob, Class<?> expType)
    {
        if (ob == null) {
            fail("Expected an object of type "+expType.getName()+", got null");
        }
        Class<?> cls = ob.getClass();
        if (!expType.isAssignableFrom(cls)) {
            fail("Expected type "+expType.getName()+", got "+cls.getName());
        }
    }

    protected void verifyException(Throwable e, String... matches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : matches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("+Arrays.asList(matches)+"): got one with message \""+msg+"\"");
    }

}
