#!/usr/bin/env python
"""
generates aut for jtorx from dot file learned with learnlib
  '?COIN_1_1'
  '!TEA_0_1'

note: dot file uses I for input instead of ? and O for output instead of !
"""
# Author: Harco Kuppens


import sys, re, pprint  # modules from standard lib (python 2.6 and later)



def get_lts_from_dotfile(dot_file):
    """ Get labeled transition system from graphviz dot file

        The dot file:
           - describes a digraph with labels
           - encodes the start state with the color='red' attribute
             note: this corresponds with the highlighted state in learnlib API

        Returns: [start_state,transions]
        Where :
           - start_state: start state label
           - transitions: list of transitions

    """

    start_state='unknown'
    f=file(dot_file)
    lines=f.readlines()

    # find start state
    for line in lines:
        line=line.replace('label=""','')
        if line.find('[color="red"]') != -1:
            start_state=line[:line.find(' ')]
            break


    # get transitions
    transitions=[]
    for line in lines:
        if line.find('->') != -1:
            transitions.append(line)

    # throw away transitions with the keywords : quiescence or inconsistency or undefined
    #transitions = [ t for t in transitions if ( 'quiescence' not in t ) and ( 'inconsistency' not in t  )  and ( 'undefined' not in t  )]

    trans_out=[]
    regexpr_transition=re.compile(r'\s*(\w*)\s*-\>\s*(\w*)\s*\[label=\<(.*)\>\]')
    regexpr_tag=re.compile(r'<[^>]+>')
    for transition in transitions:
        match=regexpr_transition.match(transition)
        if match:
            match=match.groups()
            label=regexpr_tag.sub('',match[2])
            trans_out.append({
              'source' : match[0],
              'target' : match[1],
              'label': label
            })

    states=set()
    for t in trans_out:
        states.add(t['source'])
        states.add(t['target'])


    return [start_state,states,trans_out]


def parse_labels_of_mealy_lts(transitions):
    """Parse labels of labeled transition system
    """
    trans_out=[]
    for t in transitions:
        label=t['label']
        [inputstr,outputstr]=label.split('/')
        trans_out.append({
          'source' : t['source'],
          'target' : t['target'],
          'input':  inputstr,
          'output':  outputstr,
        })
    return trans_out

def split_io_transitions_in_separate_input_and_output_transition(io_transitions,nr_states):
    """Split transitions with both an input and output event into two transitions

       Makes two sequential transitions with a dummy state in between:
        - dummy state <midstate> is labeled :
             m_<counter>
        - first transition :
             <source> -> <midstate>  for  <input>
        - second transition :
             <midstate> -> <target>  for <output>
    """
    trans_out=[]
    id=nr_states
    for t in io_transitions:
        midstate= 'm' + str(id)
        trans_out.append({
            'source': t['source'],
            'target': midstate,
            'label' : "?" + t['input'][1:],
        })
        trans_out.append({
            'source': midstate,
            'target': t['target'],
            'label' : "!" + t['output'][1:],
        })
        id=id+1

    states=set()
    for t in trans_out:
        states.add(t['source'])
        states.add(t['target'])

    return [states,trans_out]


def transitions2aut(transitions,first_state,nr_of_states):
    nr_of_transitions=len(transitions)
    strings=[ "des(" + first_state[1:] + ","  + str(nr_of_transitions) + "," + str(nr_of_states) +")"]
    for t in transitions:
       #aut_edge ::= "(" start_state "," label "," end_state ")"
       strings.append("("+t['source'][1:] + "," + '"' + t['label'] + '"' + "," + t['target'][1:] + ")" )

    return "\n".join(strings)


def dot2aut(dot_filename_in):
    """
       from mealy machine in a .dot file written by DotUtil.write of learnlib
       we create an .aut file containing an lts where input and output each
       have its own labeled transition. An input transition has a
       label starting with '?' and an output transition has a label
       starting with '!'

    """

    if  dot_filename_in[-4:].lower() != '.dot':
       print "Problem: file '"+ dot_filename_in + "' is not a dot file!!"
       print "Exit!"
       sys.exit(1)


    [start_state,states,transitions]=get_lts_from_dotfile(dot_filename_in)
    io_transitions=parse_labels_of_mealy_lts(transitions) # each transition has input and output
    [states,transitions]=split_io_transitions_in_separate_input_and_output_transition(io_transitions,len(states)) # each transition only has label again


    result=transitions2aut(transitions,start_state,len(states))

    aut_filename=dot_filename_in[:-4] + ".aut"

    f=open(aut_filename ,'w')
    f.write(result)
    f.close()

    print "written file : " +  aut_filename


if __name__ == "__main__":
    dot2aut(*sys.argv[1:])

