package ws.nmathe.saber.commands.general;

import ws.nmathe.saber.Main;
import ws.nmathe.saber.commands.Command;
import ws.nmathe.saber.core.schedule.ScheduleEntry;
import ws.nmathe.saber.core.schedule.ScheduleManager;
import ws.nmathe.saber.core.schedule.ScheduleEntryParser;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ws.nmathe.saber.utils.MessageUtilities;
import ws.nmathe.saber.utils.ParsingUtilities;
import ws.nmathe.saber.utils.VerifyUtilities;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;

/**
 */
public class EditCommand implements Command
{
    private String prefix = Main.getBotSettings().getCommandPrefix();
    private ScheduleManager schedManager = Main.getScheduleManager();

    @Override
    public String help(boolean brief)
    {
        String USAGE_EXTENDED = "The entry's title, start time, start date, end time, comments," +
                " and repeat may be reconfigured with this command using the form **!edit <ID> <option> <arguments>**\n The" +
                " possible arguments are **title \"NEW TITLE\"**, **start h:mm**, **end h:mm**, **date MM/dd**, " +
                "**repeat no**/**daily**/**weekly**, and **comment add \"COMMENT\"** (or **comment remove**). When " +
                "removing a comment, either the comment copied verbatim (within quotations) or the comment number needs" +
                " to be supplied.";

        String EXAMPLES = "Ex1: **!edit 3fa0 comment add \"Attendance is mandatory\"**" +
                "\nEx2: **!edit 0abf start 21:15**" +
                "\nEx3: **!edit 49af end 2:15pm**" +
                "\nEx4: **!edit 80c0 comment remove 1**";

        String USAGE_BRIEF = "**" + prefix + "edit** - Modifies an event entry, either" +
                " changing botSettings or adding/removing comment fields.";

        if( brief )
            return USAGE_BRIEF;
        else
            return USAGE_BRIEF + "\n\n" + USAGE_EXTENDED + "\n\n" + EXAMPLES;
    }

    @Override
    public String verify(String[] args, MessageReceivedEvent event)
    {
        if( args.length < 3 )
            return "Not enough arguments";

        // check first arg
        if( !VerifyUtilities.verifyHex(args[0]) )
            return "ID \"" + args[0] + "\" is not a valid ID value";

        // check later args
        switch( args[1].toLowerCase() )
        {
            case "comment":
                switch (args[2])
                {
                    case "add":
                        if (!VerifyUtilities.verifyString(Arrays.copyOfRange(args, 3, args.length)))
                            return "Invalid argument \"" + args[3] + "\"";
                        break;
                    case "remove":
                        if(Character.isDigit(args[3].charAt(0)) &&
                                !VerifyUtilities.verifyInteger(args[3]))
                            return "Invalid argument \"" + args[3] + "\"";
                        else
                        {
                            if (!VerifyUtilities.verifyString(Arrays.copyOfRange(args, 3, args.length)))
                                return "Invalid argument \"" + args[3] + "\"";
                        }
                        break;
                    default:
                        return "Invalid argument \"" + args[2] + "\"";
                }
                break;

            case "start":
                if(args.length > 3)
                    return "Not enough arguments";
                if( !VerifyUtilities.verifyTime( args[2] ) )
                    return "Invalid argument \"" + args[2] + "\"";
                break;

            case "end":
                if(args.length > 3)
                    return "Not enough arguments";
                if( !VerifyUtilities.verifyTime( args[2] ) )
                    return "Invalid argument \"" + args[2] + "\"";
                break;

            case "title":
                String[] comment = Arrays.copyOfRange( args, 2, args.length );
                if(!VerifyUtilities.verifyString(comment))
                    return "Invalid argument \"" + args[2] + "\"";
                break;

            case "date":
                if(args.length > 3)
                    return "Not enough arguments";
                if( !VerifyUtilities.verifyDate( args[2] ) )
                    return "Invalid argument \"" + args[2] + "\"";
                break;

            case "repeat":
                if(args.length > 3)
                    return "Not enough arguments";
                if( !VerifyUtilities.verifyRepeat(args[2]) )
                    return "Invalid argument \"" + args[2] + "\"";
                break;
        }

        return "";
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event)
    {
        // parseMsgFormat argument into the event entry's ID
        Integer entryId = Integer.decode( "0x" + args[0] );

        // check if the entry exists
        ScheduleEntry entry = schedManager.getEntry( entryId );
        if( entry == null  || !entry.eMsg.getGuild().getId().equals(event.getGuild().getId()) )
        {
            String msg = "There is no event entry with ID " + Integer.toHexString(entryId) + ".";
            event.getChannel().sendMessage( msg ).queue();
            return;
        }

        String title = entry.eTitle;
        ArrayList<String> comments = entry.eComments;
        ZonedDateTime start = entry.eStart;
        ZonedDateTime end = entry.eEnd;
        int repeat = entry.eRepeat;

        switch( args[1] )
        {
            case "comment":
                if( args[2].equals("add") )
                {
                    String comment = "";
                    for( int i = 3; i < args.length; i++ )
                    {
                        comment += args[i].replace("\"", "");
                        if (!(i == args.length - 1))
                            comment += " ";
                    }
                    comments.add( comment );
                }
                else if( args[2].equals("remove"))
                {
                    if( args[3].charAt(0)=='\"' )
                    {
                        String comment = "";
                        for (int i = 3; i < args.length; i++) {
                            comment += args[i].replace("\"", "");
                            if (!(i == args.length - 1))
                                comment += " ";
                        }
                        comments.remove(comment);
                    }
                    else if( Integer.parseInt(args[3]) <= comments.size() )
                        comments.remove( Integer.parseInt(args[3])-1 );
                }
                break;

            case "start":
                start = ParsingUtilities.parseTime( start, args[2] );
                break;

            case "end":
                end = ParsingUtilities.parseTime( end, args[2] );
                break;

            case "title":
                title = "";
                for( int i = 2; i < args.length; i++ )
                {
                    title += args[i].replace("\"", "");
                    if(!(i == args.length-1))
                        title += " ";
                }
                break;

            case "date":
                start = start.withMonth(Integer.parseInt(args[2].split("/")[0]));
                start = start.withDayOfMonth(Integer.parseInt(args[2].split("/")[1]));
                end = end.withMonth(Integer.parseInt(args[2].split("/")[0]));
                end = end.withDayOfMonth(Integer.parseInt(args[2].split("/")[1]));
                if( end.isBefore(start) )
                {
                    end.plusDays(1);
                }
                break;

            case "repeat":
                if( Character.isDigit( args[2].charAt(0) ))
                    repeat = Integer.parseInt(args[2]);
                else if( args[2].equals("daily") )
                    repeat = 1;
                else if( args[2].equals("weekly") )
                    repeat = 2;
                else if( args[2].equals("no") )
                    repeat = 0;
                break;
        }

        synchronized( schedManager.getScheduleLock() )
        {
            schedManager.removeEntry( entryId );
        }

        String msg = ScheduleEntryParser.generate(title, start, end, comments, repeat, entryId, entry.eMsg.getGuild().getId());
        MessageUtilities.editMsg(msg, entry.eMsg, schedManager::addEntry);
    }
}