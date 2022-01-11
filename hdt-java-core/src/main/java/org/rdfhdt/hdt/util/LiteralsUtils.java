package org.rdfhdt.hdt.util;

import org.apache.jena.graph.Node;
import org.rdfhdt.hdt.rdf.parsers.JenaNodeCreator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiteralsUtils {
    static Pattern pattern = Pattern.compile("\".*\"\\^\\^<.*>");


    public static boolean containsLanguage(String str){
        Node node = JenaNodeCreator.createLiteral(str);
        String lang = node.getLiteralLanguage();
        return !lang.equals("");
    }
    public static String getType(CharSequence str){

        Node node;
        char firstChar = str.charAt(0);
        // TODO split blank nodes as well in a seperate section
//        if(firstChar=='_') {
//            node = JenaNodeCreator.createAnon(str.toString());
//        }
        if(firstChar=='"') {
            node = JenaNodeCreator.createLiteral(str.toString());
            String dataType = node.getLiteralDatatypeURI();
            return "<"+dataType+">";
        }else{
            return "NO_DATATYPE";
        }

//        Matcher matcher = pattern.matcher(str);
//        String dataType;
//        if(matcher.find()){
//            dataType = str.toString().split("\\^")[2];
//        }else{
//            dataType = "NO_DATATYPE";
//        }
//        return dataType;
    }
    public static String removeType(CharSequence str){
        String res = "";
//        char firstChar = str.charAt(0);
//        if(firstChar == '"'){
//            Node node = JenaNodeCreator.createLiteral(str.toString());
//            res = node.getLiteralValue().toString();
//            String str1 = node.getLiteral().toString();
//            return res;
//        }
//        return str.toString();
        Matcher matcher = pattern.matcher(str);
        if(matcher.matches()){
            String temp = str.toString();
            int index = temp.lastIndexOf("^");
            res = temp.substring(0,index-1);

            //res = str.toString().split("\\^")[0];
        }else{
            res = str.toString();
        }
        return res;
    }
}
