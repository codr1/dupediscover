package com.codr1.dupediscover;
//import com.google.common.collect.ArrayListMultimap;
//import com.google.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;

import com.vaadin.annotations.Push;
import javax.servlet.annotation.WebServlet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.data.Item;
import com.vaadin.ui.UI;
import com.vaadin.ui.Table;
import com.vaadin.data.util.FilesystemContainer;
import java.io.File;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.TextField;
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
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
    
    Multimap<String,File> allFiles; 
    
    FilenameFilter fileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            File file = new File( dir.getAbsolutePath() + "/" + name );
            return file.isDirectory();
            
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    void processFile( final Path path ) {
        final File file = path.toFile();
         
        numberFilesScanned++;
        currentTime = System.currentTimeMillis() / 500;
        if( currentTime > oldTime ){ 
            access( new Runnable() {
                @Override
                public void run() {
                    currentFileBox.setValue(file.toString());
                    numberFilesBox.setValue( numberFilesScanned.toString() );
                }
            });
            oldTime = currentTime;
        }
       
        
        // if this is a directory we don't need it.
        if( file.isDirectory() ) {
            return;
        }
        allFiles.put( file.getName(), file );
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
            
            // When the scan is finished, lets update the UI. 
            access( new Runnable() {
                    @Override
                    public void run() {
                        Notification.show( "Scan Completed!" );

                        startScan.setEnabled(true);
                        cancelScan.setEnabled(false);
                        currentFileBox.setValue("Scan Completed");
                        numberFilesBox.setValue( numberFilesScanned.toString() );
                    }
                });
        }
    }
            
    FilesystemContainer files = new FilesystemContainer( new File( "/" ), fileFilter, false );
    TreeTable availableDirectories = new TreeTable( "Directory Tree", files );
    Table selectedDirectories = new Table( "Selected Directories" );
    TreeTable foundDuplicates = new TreeTable( "Found Duplicates ");
    final Button startScan = new Button( "Start Scan" );
    final Button cancelScan = new Button( "Cancel Scan" );
    final TextField currentFileBox = new TextField();
    final TextField numberFilesBox = new TextField();
    
    Thread scanThread;
    
    private void startScan() {
        scanThread = new ScanThread();
            
        scanThread.start();
    }
    
    
    @Override
    protected void init(VaadinRequest vaadinRequest) {
        VerticalSplitPanel vSplitLeft = new VerticalSplitPanel();
        VerticalSplitPanel vSplitRight = new VerticalSplitPanel();
        GridLayout topRightGrid = new GridLayout( 5, 2 );
        VerticalLayout topLeftVertical = new VerticalLayout();
        VerticalLayout bottomLeftVertical = new VerticalLayout();
        
        HorizontalSplitPanel hSplit = new HorizontalSplitPanel(vSplitLeft, vSplitRight);
        
        selectedDirectories.addContainerProperty( "Name", String.class, null );
        
        vSplitLeft.addComponent( topLeftVertical );
        vSplitRight.addComponent( topRightGrid );
        vSplitLeft.addComponent( bottomLeftVertical );
        
        topLeftVertical.addComponent(availableDirectories);
        
        topRightGrid.addComponent(selectedDirectories, 0, 0, 4, 0);     
       
        topRightGrid.addComponent( startScan, 0, 1, 0, 1 );
        topRightGrid.addComponent( cancelScan, 1, 1, 1, 1 );
        topRightGrid.addComponent( numberFilesBox, 2, 1, 2, 1 );
        topRightGrid.addComponent( currentFileBox, 3, 1, 4, 1 );
        selectedDirectories.setWidth("100%");
        selectedDirectories.setHeight("100%");
        topRightGrid.setHeight("100%");
        topRightGrid.setWidth("100%");
        currentFileBox.setWidth( 36, Unit.EM);
        topRightGrid.setRowExpandRatio(0, 1);
        
        availableDirectories.setVisibleColumns("Name", "Last Modified");
        availableDirectories.setColumnExpandRatio( "Name", 1 );
        availableDirectories.setColumnExpandRatio( "Last Modified", 0 );
        
        topLeftVertical.setHeight("100%");
           
        availableDirectories.setWidth("100%");
        availableDirectories.setHeight("100%");
        
        
        
        bottomLeftVertical.addComponent( foundDuplicates );
        foundDuplicates.setHeight( "100%" );
        foundDuplicates.setWidth( "100%" );
        
        foundDuplicates.addContainerProperty("File Name", String.class, null);
        foundDuplicates.addContainerProperty("Directory", String.class, null);
        foundDuplicates.addContainerProperty("Size", Integer.class, null);
        
        setContent( hSplit );
        
        final Notification notify = new Notification("hi");
        notify.setDelayMsec(20000);
        
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
                            if( currentDirectory.contains(itemToAdd.toString())){
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
                                
                try{
                    scanThread.join();
                } catch (InterruptedException e ){
                    notify.show( "Getting interrupted up in here!" );
                }
                
                currentFileBox.setValue( "Looking for duplicates..." );
                
                Set keySet = allFiles.keySet();
                Iterator keyIter = keySet.iterator();
                while( keyIter.hasNext() ){
                    String key = (String) keyIter.next();
                    Collection<File> values = allFiles.get( key );
                    if( values.size() > 1 ) {
                        for( File currentFile : values ) {
                            foundDuplicates.addItem( new Object[]{currentFile.toString(), null, currentFile.length() } );
                        }  
                    }
                    
                    
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
