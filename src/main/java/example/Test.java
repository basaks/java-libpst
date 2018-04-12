package example;

import java.util.HashMap;
import java.util.Vector;
import java.io.FileWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.pff.*;


public class Test {
    public static void main(final String[] args) {
        new Test(args[0]);
    }

    private int depth = -1;
    private String filename;

    public Test(final String filename) {
        try {
            final PSTFile pstFile = new PSTFile(filename);
            this.filename = filename;
            System.out.println(pstFile.getMessageStore().getDisplayName());

            this.processFolder(pstFile.getRootFolder());
        } catch (final Exception err) {
            err.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void processFolder(final PSTFolder folder) throws PSTException,
            java.io.IOException {
        this.depth++;
        // the root folder doesn't have a display name
        if (this.depth > 0) {
            this.printDepth();
            System.out.println(folder.getDisplayName());
        }

        JSONObject obj = new JSONObject();
        obj.put("name", folder.getDisplayName());

        // go through the folders...
        if (folder.hasSubfolders()) {
            final Vector<PSTFolder> childFolders = folder.getSubFolders();
            for (final PSTFolder childFolder : childFolders) {
                this.processFolder(childFolder);
            }
        }


        // and now the emails for this folder
        if (folder.getContentCount() > 0) {

            JSONArray emails =  new JSONArray();

            this.depth++;
            PSTMessage email = (PSTMessage) folder.getNextChild();


            while (email != null) {
                System.out.println("===================================");
                HashMap<String, String> msg =  new HashMap<>();
                this.printDepth();
                System.out.println("Subject: " + email.getSubject());
                msg.put("Subject", email.getSubject());
                this.printDepth();
                if (email.hasAttachments()) {
                    for (int att = 0; att < email.getNumberOfAttachments();
                         att++) {
                        System.out.println("Attachment number: " + att + 1);
                        PSTAttachment attachment = email.getAttachment(att);
                        this.printDepth();
                        System.out.println(
                                attachment.getLongFilename() + '-' +
                                attachment.getSize() + '-' + attachment
                                .getFilesize() + '-' +
                        attachment.getAttachmentContentDisposition());
                    }
                    msg.put("Attachments",  Integer.toString(email
                            .getNumberOfAttachments()));
                }
                else {
                    msg.put("Attachment", "0");
                }
                System.out.println("Author Email: "
                        + " - " + email.getSentRepresentingEmailAddress());

                msg.put("Sender", email.getSentRepresentingEmailAddress
                        ());
                this.printDepth();
                System.out.println("Author: " + email.getSentRepresentingName()
                        + ", Recipient: " + email.getDisplayTo());
                msg.put("From", email.getSentRepresentingName());
                msg.put("RcvdRepresentingEmailAddress",
                        email.getRcvdRepresentingEmailAddress());
                this.printDepth();
                msg.put("To", email.getDisplayTo());
                System.out.println("To: " + email.getDisplayTo());

                this.printDepth();
                msg.put("CC", email.getDisplayCC());
                System.out.println("CC: " + email.getDisplayCC());


                msg.put("BCC", email.getDisplayBCC());
                String body = email.getBody();
                msg.put("Contents", body);
                this.printDepth();
                System.out.println("Contents: " + body);
                this.printDepth();
                msg.put("NoOfRecipients", Integer.toString(email
                        .getNumberOfRecipients()));
                this.printDepth();
                System.out.println("ClientSubmitTime: " + email
                        .getClientSubmitTime());
                msg.put("ClientSubmitTime", email.getClientSubmitTime()
                        .toString());
                this.printDepth();
                System.out.println("MessageDeliveryTime: " + email
                        .getMessageDeliveryTime());

                msg.put("MessageDeliveryTime", email.getMessageDeliveryTime()
                        .toString());
                msg.put("Conversation",
                        this.mapConversation(body).toString());
                this.printDepth();
                email = (PSTMessage) folder.getNextChild();

                emails.add(msg);
            }
            this.depth--;

            try (FileWriter file = new FileWriter(this.filename + folder
                    .getDisplayName() + ".json")) {
//                Check
//                for (int e = 0; e < emails.size(); e ++ ){
//                    System.out.println("msg: " + emails.get(e));
//                }

                obj.put("messages", emails);
                file.write(obj.toJSONString());
                System.out.println("Successfully Copied JSON " +
                        "Object to File: " + this.filename + "_" +
                        folder.getDisplayName() + ".json");
            }

        }

        this.depth--;
    }

    @SuppressWarnings("unchecked")
    private HashMap mapConversation(String body) {
        String MESSAGE = "Message";
        String PARTICIPANT_ENTERED = "Participant Entered";
        String PARTICIPANT_LEFT = "Participant Left";

        HashMap<String, JSONArray> conv =  new HashMap<>();
        String[] allFields = body.split("\n");
        JSONArray messages = new JSONArray();
        JSONArray participantsEntered = new JSONArray();
        JSONArray participantsLeft = new JSONArray();
        for(String item : allFields){
            item = item.trim();
            System.out.println("printing item:" + item);
            String[] keyval = item.split(":", 2);
            if (keyval.length > 1) {
                if (keyval[0].equals(MESSAGE)) {
                    messages.add(keyval[1]);
                } else if (keyval[0].equals(PARTICIPANT_ENTERED)) {
                    participantsEntered.add(keyval[1]);
                } else if (keyval[0].equals(PARTICIPANT_LEFT)) {
                    participantsLeft.add(keyval[1]);
                }
            }
        }
        conv.put(PARTICIPANT_ENTERED, participantsEntered);
        conv.put(PARTICIPANT_LEFT, participantsLeft);
        conv.put(MESSAGE, messages);

        conv.forEach((k, v) -> System.out.println(k + "=" + v));
        return conv;
    }

    private void printDepth() {
        for (int x = 0; x < this.depth - 1; x++) {
            System.out.print(" | ");
        }
        System.out.print(" |- ");
    }
}
