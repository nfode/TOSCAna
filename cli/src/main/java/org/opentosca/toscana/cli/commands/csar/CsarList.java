package org.opentosca.toscana.cli.commands.csar;

import org.opentosca.toscana.cli.commands.AbstractCommand;

import picocli.CommandLine.Command;

@Command(name = "list",
    description = {"Show all uploaded CSARs"},
    customSynopsis = "@|bold toscana csar list|@ [@|yellow -mv|@]%n")
public class CsarList extends AbstractCommand {

    /**
     Get's called if the available CSARs should be printed
     */
    public CsarList() {
    }

    @Override
    public void run() {
        System.out.println(getApi().listCsar());
    }
}
