1.	Description

The MetadataEngine is tool to generate associated metadata files. It accomplishes the following tasks:
•	In general, it fills the given velocity template.
•	Contents are
    o	static text files of the format *.properties or *.xml
    o	the metadata files associated to the given input items (e.g. products)
    o	input item paths
    o	the Java System class
    o	all additional CLI arguments in order
•	The generated metadata file is placed next to the target item (e.g. product).


2.	Metadata File Naming Convention

There is a naming convention how associated metadata files are found (for input items). The same rule applies
to the automatic naming of the metadata file generated at that instance. Associated metadata are following the pattern

<itemName>-<baseOfVelocityTemplate>.<suffixFromVelocityTemplate>.

An example: My item is called ‘MER_RR_something.N1’. My velocity template is called ‘meta.xml.vm’.
The associated metadata file will be called: ‘MER_RR_something-meta.xml’.
