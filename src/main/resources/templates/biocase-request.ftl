<?xml version='1.0' encoding='UTF-8'?>
<request xmlns='http://www.biocase.org/schemas/protocol/1.3' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
         xsi:schemaLocation='http://www.biocase.org/schemas/protocol/1.3 http://www.bgbm.org/biodivinf/Schema/protocol_1_3.xsd'>
    <header>
        <type>search</type>
    </header>
    <search>
        <requestFormat>${contentNamespace}</requestFormat>
        <responseFormat start="${startAt}" limit="${limit}">${contentNamespace}</responseFormat>
        <count>false</count>
    </search>
</request>