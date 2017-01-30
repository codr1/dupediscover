package com.codr1.dupediscover;
import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;
import com.vaadin.annotations.Push;
import javax.servlet.annotation.WebServlet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.UI;
import com.vaadin.ui.Table;
import com.vaadin.data.util.FilesystemContainer;
import com.vaadin.data.util.TextFileProperty;
import com.vaadin.server.FileResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractField;
import java.io.File;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.TextField;
import static java.io.File.separatorChar;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.filechooser.FileSystemView;
import org.vaadin.dialogs.ConfirmDialog;

/**
 *
 */
@Push
@Theme("mytheme")
@Widgetset("com.codr1.dupediscover.MyAppWidgetset")

public class DupeDiscoverUI extends UI {
    static long currentTime = System.currentTimeMillis() / 1000;
    static long oldTime = System.currentTimeMillis() / 1000; 
    static Integer  numberFilesScanned = 0;
    
    ArrayListMultimap<String,File> allFiles; 
    
    VerticalSplitPanel vSplitLeft;
    VerticalSplitPanel vSplitRight;
    GridLayout topRightGrid;
    VerticalLayout topLeftVertical;
    VerticalLayout bottomLeftVertical;
    VerticalLayout bottomRightVertical;

    HorizontalSplitPanel hSplit;
    
    FilenameFilter fileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            File file = new File( dir.getAbsolutePath() + "/" + name );
            return file.isDirectory();
            
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    FilesystemContainer files; 
    TreeTable availableDirectories = new TreeTable( "Directory Tree", files );
    Table selectedDirectories = new Table( "Selected Directories" );
    TreeTable foundDuplicates = new TreeTable( "Found Duplicates ");
    final Button startScan = new Button( "Start Scan" );
    final Button cancelScan = new Button( "Cancel Scan" );
    final TextField currentFileBox = new TextField();
    final TextField numberFilesBox = new TextField();

    Label preview = new Label( "", ContentMode.HTML );
    Image previewImage;

    Thread scanThread;

    
    void showInitialDialog() {
        String confirmMsg = 
                "WARNING!  IMPROPER USE OF THIS PROGRAM COULD DAMAGE YOUR SYSTEM!!!<br>" +
                "<b>PROCEED AT YOUR OWN RISK!  IF YOU ARE NOT COMFORTABLE PLEASE QUIT RIGHT AWAY!</b></br><br>" +
                "This program is meant to help with photo and other file organization and deduplication.  "+
                "Even the most orgazined people with well structured photo repositories, often have "+
                "many camera dumps and phone backups living on various systems.  Cleaning those out safely "+
                "every several months is difficult and very very time consuming.<br>"+
                "This program helps you identify duplicates quickly and choose the master copy in bulk.<br> "+
                "<ol><li> Select directories to scan.  The list of selected directories appars in the top right pane. "+
                "<li> Click the 'Scan' button.  The program will scan recursivelly and identify all duplicates " +
                "with the exact name and size match.  They will appear in the bottom left window."+
                "<li> Select the 'Master' copy of a file.</ol><br>" +
                "<b><i> At this time the program will walk throught the directory of the 'master' "+
                "as well as those of the non-master.  It will delete any file in the non-master directories "+
                "that is duplicated (has the exact same name and size) in the master directory. " +
                "Files that are not duplicated in teh master will not be touched.<br>" +
                
                "The original in the master will always remain (the program will never delete all copies of a file).<br>"+
                "</i></b><br>"+
                "<b> AT THIS TIMES DUPLICATE FILES WILL BE IREVOCABLY DELETED!  EXCERCISE CAUTION!</b>"+
                "";
               
        ConfirmDialog.show(this, "Usage", confirmMsg, "Accept and Continue",  "Exit", new ConfirmDialog.Listener() {
            @Override
            public void onClose(ConfirmDialog cd) {
                if( cd.isConfirmed() ){
                    //Nothing happens 
                   
                } else {
                    Notification.show("Thank you!  Please close this window");
                    System.exit(0);
                }
            }
        }).setContentMode(ConfirmDialog.ContentMode.HTML);
    }
    
    void populateFileSystems( ) {
        File[] paths;
        FileSystemView fsv = FileSystemView.getFileSystemView();
        String previousValue;
        
        // returns pathnames for files and directory
        paths = File.listRoots();   

        for( File path : paths ){ 
            if( files == null ) {
                files = new FilesystemContainer( path, false );
            } else {
                files.addRoot( path );
            }
        }
        
    /*    // if there is no previous Value, or the previous value is no longer in the list
        // get the first item in the list.  Otherwise try to select the previously selected value
        if( previousValue.isEmpty() || !pathsStringArray.contains( previousValue )){
            fileSystems.select( pathsStringArray.get(0) );
        } else {
            
            fileSystems.setValue( previousValue );
        }
        files = new FilesystemContainer( new File( fileSystems.getValue().toString() ), fileFilter, false );
        */
        availableDirectories.setContainerDataSource( files );
        
        //fileSystems.setNullSelectiofinAllowed(false);
        
      
    }
    
    //This function is a wrapper which allows threads to update the main UI.
    void updateStatusFromThread( final AbstractField field, final String text ){
        access( new Runnable() {
            @Override
            public void run() {
                field.setValue( text );
                
            }
        });
    }
    
    void processFile( final Path path ) {
        final File file = path.toFile();
         
        numberFilesScanned++;
        currentTime = System.currentTimeMillis() / 500;
        if( currentTime > oldTime ){ 
            updateStatusFromThread( currentFileBox, file.toString());
            updateStatusFromThread(numberFilesBox, numberFilesScanned.toString());
            
            oldTime = currentTime;
        }
       
        
        // if this is a directory we don't need it.
        if( file.isDirectory() ) {
            return;
        }
        allFiles.put( file.getName() + "/" + file.length() , file );
    }
   
    public /*static*/ class Finder 
            extends SimpleFileVisitor<Path> {
        
        
        
        // Process each file .
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            processFile (file);
            if( Thread.currentThread().isInterrupted() ){
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }
        
        // Handle error
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            System.err.println(exc);
            if( Thread.currentThread().isInterrupted() ){
                return FileVisitResult.TERMINATE;
            }
            return CONTINUE;   
        }
    }
    
    
    class ScanThread extends Thread {
        
        @Override
        public void run() {
            // Check if there is anything to scan
            if( selectedDirectories.size() < 1 ) {
                access( new Runnable() {
                    @Override
                    public void run() {
                        Notification.show( "No directories have been selected to be scanned" );
                        startScan.setEnabled(true);
                        cancelScan.setEnabled(false);
                    }
                });
                return;
            } else {
                access( new Runnable() {
                    @Override
                    public void run() {
                        bottomLeftVertical.removeComponent(foundDuplicates);
                        buildFoundDuplicatesTable();
                        
                        // we are not going to add it back in quite yet.  lets see if it is faster that way
                        //bottomLeftVertical.addComponent(foundDuplicates);   
                    }
                });
            }
            numberFilesScanned = 0;
            
            //Iterate through the slected directories
            for( Iterator i = selectedDirectories.getItemIds().iterator(); i.hasNext();){
                Item currentRow = selectedDirectories.getItem( (Integer) i.next() );            
                String currentDirectory = currentRow.getItemProperty("Name").getValue().toString();
                Path path = Paths.get(currentDirectory);

                // Try to follow all links
                EnumSet<FileVisitOption> opts = EnumSet.of(FOLLOW_LINKS);

                Finder finder = new Finder();
                try {
                    Files.walkFileTree( path, opts, Integer.MAX_VALUE, finder );
                } catch ( IOException e ) {
                    System.err.println( "we got an IOException" + e );
                }
            }
            
            updateStatusFromThread( currentFileBox, "Parsing Results...  This could take a bit");
            updateStatusFromThread(numberFilesBox, numberFilesScanned.toString() );
            


            currentFileBox.setValue( "Parsing Results..." );

            //Set keySet = allFiles.keySet();
           // Set entrySet = allFiles.asMap().entrySet();
           
            Integer i = 0;
            Integer rowsAdded = 0;
            for( Map.Entry<String, Collection<File>> entry : allFiles.asMap().entrySet() ) {
                String key = entry.getKey();
                Collection<File> values = entry.getValue();

                if( values.size() > 1 ) {
                    Object newParent = null;
                    Object newRow;
                    for( File currentFile : values ) {
                        String currentFileName = currentFile.getName();

                        // Handle the first row (the parent row)
                        if( newParent == null ) {
                            newParent = foundDuplicates.addItem( new Object[]{currentFileName, null, null }, null );
                            foundDuplicates.setCollapsed(newParent, false);
                        }

                        newRow = foundDuplicates.addItem( new Object[]{currentFileName, currentFile.getParent(), currentFile.length() }, 
                                currentFile.getAbsolutePath() );
                        foundDuplicates.setParent( newRow, newParent );                  
                    }
                    rowsAdded++;
                    if( rowsAdded % 1000 == 0 ){
                        final Integer rowsAddedf = rowsAdded;
                        access( new Runnable() {
                            @Override
                            public void run() {
                                Notification.show( rowsAddedf + " Added " );
                                startScan.setEnabled(true);
                                cancelScan.setEnabled(false);
                            }
                        });
                    }
                }
                i++;
                currentTime = System.currentTimeMillis() / 500;
                if( currentTime > oldTime ){ 
                    updateStatusFromThread(numberFilesBox, i.toString() + "/" + allFiles.size());
                    oldTime = currentTime;
                }
            }    
           
            
            // When the scan is finished, lets update the UI. 
            access( new Runnable() {
                @Override
                public void run() {
                    Notification.show( "Scan Completed!" );
                    //add the table back in
                    bottomLeftVertical.addComponent(foundDuplicates);   
                    startScan.setEnabled(true);
                    cancelScan.setEnabled(false);
                    currentFileBox.setValue("Scan Completed");
                    numberFilesBox.setValue( numberFilesScanned.toString() );
                }
            });
        }
    }
            
    private void startScan() {
        scanThread = new ScanThread();
            
        scanThread.start();
    }
    
    private void buildFoundDuplicatesTable() {
        foundDuplicates = new TreeTable( "Found Duplicates" );
        foundDuplicates.setHeight( "100%" );
        foundDuplicates.setWidth( "100%" );
        
        foundDuplicates.addContainerProperty("File Name", String.class, null);
        foundDuplicates.addContainerProperty("Directory", String.class, null);
        foundDuplicates.addContainerProperty("Size", Long.class, null);
        
        foundDuplicates.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                Property prop = event.getProperty();
                if( prop == null ) {
                    Notification.show( "Try again!");
                    return;
                }
                
                if( prop.getValue() == null ) {
                    Notification.show( "...chill, bro!");
                    return;
                }
                
                String rowId = prop.getValue().toString(); 
                Item selectedRow = foundDuplicates.getItem(rowId );
                
                if( selectedRow == null || selectedRow.getItemProperty("Directory") == null /*|| selectedRow.getItemProperty("Directory").getValue() == null*/ ){
                    Notification.show( "Please select an entry with a file and directory" );
                    return;
                }
                
                String fileName = selectedRow.getItemProperty("Directory").getValue().toString()+ File.separator +selectedRow.getItemProperty("File Name").getValue().toString();
                
                if( preview != null && bottomRightVertical.getComponentIndex(preview) != -1 ){
                    bottomRightVertical.removeComponent( preview );
                    bottomRightVertical.removeAllComponents();
                }
                if( previewImage != null && bottomRightVertical.getComponentIndex(previewImage) != -1 ){
                    bottomRightVertical.removeComponent( previewImage );
                }
                //Hack to handle images
                if( fileName.toLowerCase().endsWith("png") ||  
                        fileName.toLowerCase().endsWith("jpg") || 
                        fileName.toLowerCase().endsWith("jpeg") || 
                        fileName.toLowerCase().endsWith("gif")
                ) {
                    FileResource fr = new FileResource( new File(fileName) );
                    previewImage = new Image( "Preview", fr );
                    bottomRightVertical.addComponent(previewImage);
                    previewImage.setHeight("100%");
                    previewImage.setWidth("100%");
                } else { 
                    
                    preview = new Label( "", ContentMode.HTML );
                    preview.setPropertyDataSource( new TextFileProperty(new File( fileName ) ) );
                    bottomRightVertical.addComponent(preview);
                    preview.setWidth("100%");
                }
            }
        });
        
         foundDuplicates.addGeneratedColumn("Set as Master Copy", new TreeTable.ColumnGenerator() {
            @Override
            public Object generateCell(Table source, final Object itemId, Object columnId) {
                Item currentRow = source.getItem(itemId);
                if( currentRow.getItemProperty("Directory").getValue() == null ) {
                    return "";
                }
                
                Button buttonSetMaster = new Button( "Set Master" );
                buttonSetMaster.addClickListener( new Button.ClickListener() {
                    @Override
                    public void buttonClick(Button.ClickEvent event) {               
                        cleanUpFiles( itemId );
                    }
                });
                return buttonSetMaster;
            }
        });
        foundDuplicates.setColumnExpandRatio( "Directory", 1 );
        
        foundDuplicates.setImmediate(true);
        foundDuplicates.setSelectable(true);
    }
    
    private void cleanUpFiles( Object itemId ) {
        
        Item masterRow = foundDuplicates.getItem(itemId);
        Object parentRowId = foundDuplicates.getParent(itemId);
        Item parentRow = foundDuplicates.getItem(itemId);
        String fileName = masterRow.getItemProperty("File Name").getValue().toString();
        String masterDirName =  masterRow.getItemProperty("Directory").getValue().toString();
        final ArrayList<String> finalList = new ArrayList();
      
        
        Collection cleanUpTargets =  new ArrayList(foundDuplicates.getChildren(parentRowId) );
        //remove the 'Master'
        cleanUpTargets.remove(itemId);
        
        // Create an array of all the dirs to be cleaned up
        String[] dirsToClean = new String[cleanUpTargets.size()];
        int i = 0;
        for( Iterator iterator = cleanUpTargets.iterator(); iterator.hasNext(); i++ ){
            Item currentRow = (Item) foundDuplicates.getItem( iterator.next() );
            dirsToClean[i] = currentRow.getItemProperty("Directory").getValue().toString();
        }
        
        // Go through all the files in the master directory
        File masterDirectory = new File( masterDirName );
        File[] masterFileList = masterDirectory.listFiles();
        if( masterFileList != null ){
            for( File currentFile : masterFileList ){
                // do not deal with directories
                if( currentFile.isDirectory() ){
                    continue;
                }
                
                // check if this file name exists in any of the other dirs to clean
                for( String dirToClean : dirsToClean ){
                    File fileToClean = new File( dirToClean + File.separator + currentFile.getName() );
                    if( fileToClean.exists() ){
                        finalList.add(fileToClean.getAbsolutePath());
                    }
                }
            }
            
            //Create a fonfirm message
            String confirmMsg = "The following files will be deleted:\n";
            for( String fileToDelete : finalList ){
                confirmMsg = confirmMsg + fileToDelete + "\n";
            }
            
            
            ConfirmDialog.show(this, confirmMsg, new ConfirmDialog.Listener() {
                @Override
                public void onClose(ConfirmDialog cd) {
                    if( cd.isConfirmed() ){
                        // Delete the files and the entries in the table...
                        for( String fileToDelete : finalList ){
                            if( new File( fileToDelete ).delete() ) {
                                foundDuplicates.removeItem(fileToDelete);
                            } else {
                                Notification.show( fileToDelete + " did not get deleted!  Aborting!" );
                                break;
                            }
                        }
                    } else {
                        Notification.show( "Doom Averted" );        
                    }
                }
            });
        }
    }
    
    @Override
    protected void init(VaadinRequest vaadinRequest) {
        
        // Need to detect if we are in windows.  If we are, we need to show
        // a drop-down with disk drives
        String osName = System.getProperty("os.name");
        if( osName.toLowerCase().contains( "win" ) ) {
            // TODO: Lets see how the file systems list works before we add the
            // windows check
        }
        
        populateFileSystems();
        
        vSplitLeft = new VerticalSplitPanel();
        vSplitRight = new VerticalSplitPanel();
        topRightGrid = new GridLayout( 5, 2 );
        topLeftVertical = new VerticalLayout();
        bottomLeftVertical = new VerticalLayout();
        bottomRightVertical = new VerticalLayout();
        
        hSplit = new HorizontalSplitPanel(vSplitLeft, vSplitRight);
        
        selectedDirectories.addContainerProperty( "Name", String.class, null );
        
        vSplitLeft.addComponent( topLeftVertical );
        vSplitRight.addComponent( topRightGrid );
        vSplitLeft.addComponent( bottomLeftVertical );
        vSplitRight.addComponent( bottomRightVertical );
        
        topLeftVertical.addComponent( availableDirectories );
        topRightGrid.addComponent( selectedDirectories, 0, 0, 4, 0 );     
        
        topRightGrid.addComponent( startScan, 0, 1, 0, 1 );
        topRightGrid.addComponent( cancelScan, 1, 1, 1, 1 );
        topRightGrid.addComponent( numberFilesBox, 2, 1, 2, 1 );
        topRightGrid.addComponent( currentFileBox, 3, 1, 4, 1 );
        
        selectedDirectories.setWidth( "100%" );
        selectedDirectories.setHeight( "100%" );
        topRightGrid.setHeight("100%");
        topRightGrid.setWidth("100%");
        currentFileBox.setWidth( 36, Unit.EM);
        topRightGrid.setRowExpandRatio(0, 1);
        
        
        topLeftVertical.setHeight("100%");
           
        availableDirectories.setWidth("100%");
        availableDirectories.setHeight("100%");
        
        buildFoundDuplicatesTable();
        bottomLeftVertical.addComponent( foundDuplicates );
        bottomRightVertical.addComponent( preview );
       
        setContent( hSplit );
        
        final Notification notify = new Notification("hi");
        notify.setDelayMsec(20000);
        
        availableDirectories.setImmediate( true );
        
        showInitialDialog();
        
        
        // Add a column with the file name that also handles drive letters to availbaleDirectories
        availableDirectories.addGeneratedColumn("File", new TreeTable.ColumnGenerator() {
            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                String fileName;
                
                fileName = itemId.toString();
                int index = fileName.lastIndexOf(File.separatorChar);
                if( index == ( fileName.length() - 1 ) ) {
                    return fileName;
                } else {
                    return fileName.substring( index + 1 );
                }
            }
        });
        
        // Add a column with the add button to availableDirectories
        availableDirectories.addGeneratedColumn("Add", new TreeTable.ColumnGenerator() {
            @Override
            public Object generateCell(Table source, final Object itemToAdd, Object columnId) {
                Button buttonAdd = new Button("Check for Dupes");
                buttonAdd.addClickListener(new Button.ClickListener() {
                    @Override
                    public void buttonClick(Button.ClickEvent event) {
                        
                        // In case we have to delete a row...  you can't delete while iterating,
                        // so we store the item ids in this list and delete once we are all done.
                        List<Object> toDelete = new ArrayList<Object>();
                        //This string will store the names of all deleted directories
                        String deletedDirectories = new String();
                        
                        String itemToAddName = itemToAdd.toString();
                        
                        // Check if we already selected a lower level or a higher level directory
                        for( Iterator i = selectedDirectories.getItemIds().iterator(); i.hasNext();){
                            int id = (Integer) i.next();
                            Item currentRow = selectedDirectories.getItem(id);
                            
                            String currentDirectory = currentRow.getItemProperty("Name").getValue().toString();
                            
                            // check if we have exactly the same name
                            if( itemToAddName.equals( currentDirectory ) ){
                                notify.show(" Directory already selected ");
                                return;
                            }
                            
                            // check if the new directory is a parent directory of the existing entry
                            if( new File(currentDirectory).getParent().contains(itemToAdd.toString())){
                                // we can't remove the item yet while we are iterating.  we will store it and remove it later
                                toDelete.add((Object)id);
                                deletedDirectories = deletedDirectories.concat( "\n" + currentDirectory + "\n");
                            }
                            
                            // Check if the new directory is a sub directory of the existing directory
                            ////NOTE: We cant just check names since /hi and /hi22 will have a problem 
                           ////So we check with the parent of the current dierctory
                            String parentPath = itemToAddName.substring(0, itemToAddName.lastIndexOf(File.separator));
                            if( parentPath.length() > 0 && currentDirectory.contains( parentPath )) {
                                notify.show("Not adding " + itemToAdd.toString() + " since its parent is already selected." );
                                return;
                            }
                        }
                        
                        //Check if we have any items slated for deletion and take them out
                        for (Object id : toDelete) {        
                            selectedDirectories.removeItem(id);
                        }
                        if( deletedDirectories.isEmpty() == false ){
                            notify.show( "Since we added their parent directory, the following have been removed:\n"+ deletedDirectories );
                            
                        }
                        
                        Object newRowId = selectedDirectories.addItem( );
                        Item newRow = selectedDirectories.getItem( newRowId );
                        newRow.getItemProperty("Name").setValue(itemToAddName );  
                    }
                });
                return buttonAdd;
            }
        });
        
        availableDirectories.setVisibleColumns("File", "Last Modified", "Add");
        availableDirectories.setColumnExpandRatio( "File", 1 );
        availableDirectories.setColumnExpandRatio( "Last Modified", 0 );
        
        
        selectedDirectories.addGeneratedColumn("Remove", new TreeTable.ColumnGenerator() {
            @Override
            public Object generateCell(Table source, final Object itemId, Object columnId) {
                Button buttonRemove = new Button( "Remove" );
                buttonRemove.addClickListener( new Button.ClickListener() {
                    @Override
                    public void buttonClick( Button.ClickEvent event ) {
                        selectedDirectories.removeItem(itemId);
                    }
                });
                return buttonRemove;
            }
        });
       
        startScan.addClickListener( new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                cancelScan.setEnabled(true);
                startScan.setEnabled(false);
                allFiles = ArrayListMultimap.create();
                
                // Scan thread really shouldn't be alive if I can click on this
                if( scanThread != null && scanThread.isAlive()) {
                    notify.show("Active scanning thread is alive!");
                } else {
                    startScan();
                    notify.show("Scan started!");
                }     
            }
        });
        
        
        cancelScan.addClickListener( new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                
                cancelScan.setEnabled(false);
                startScan.setEnabled(true);
                
                //Technically this should be the case... but just in case
                if( scanThread !=null && scanThread.isAlive()) {
                    scanThread.interrupt();
                    try{
                        scanThread.join();
                    } catch (InterruptedException e ){
                        notify.show( "Getting interrupted up in here!" );
                    }
                  notify.show("Successfully cancelled scan!");
                } else {
                    notify.show("Successfully cancelled scan, but no scan thread was actually running");
                }
                
            }
        });
        
        
        
        //selectedDirectories.setSelectable(true);
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = DupeDiscoverUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}
