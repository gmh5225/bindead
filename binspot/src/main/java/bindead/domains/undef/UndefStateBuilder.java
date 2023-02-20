package bindead.domains.undef;

import java.util.HashMap;
import java.util.Map;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.NumVar.FlagVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.combinators.FiniteStateBuilder;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.exceptions.DomainStateException.VariableSupportSetException;

class UndefStateBuilder extends FiniteStateBuilder {
  VarSet undefined;
  private AVLMap<FlagVar, VarSet> partitions;
  private AVLMap<NumVar, FlagVar> reverse;

  public UndefStateBuilder (UndefState ctx) {
    this.undefined = ctx.undefined;
    this.partitions = ctx.partitions;
    this.reverse = ctx.reverse;
  }

  private static FlagVar freshFlag () {
    return NumVar.freshFlag("e");
  }

  protected boolean isFlag (FlagVar flag) {
    return partitions.contains(flag);
  }

  private boolean contains (NumVar variable) {
    return undefined.contains(variable) || reverse.contains(variable);
  }

  /**
   * Return <code>true</code> if the variable is in the undefined set.
   */
  boolean isUndefined (NumVar var) {
    return undefined.contains(var);
  }

  /**
   * Return <code>true</code> if the variable is neither in the undefined set nor in any partition.
   */
  private boolean isDefined (NumVar var) {
    return !contains(var);
  }

  /**
   * Return <code>true</code> if any of the variables in the passed in set is undefined.
   */
  protected boolean hasUndefined (VarSet variables) {
    for (NumVar var : variables) {
      if (isUndefined(var))
        return true;
    }
    return false;
  }

  protected void addUndefined (NumVar var) {
    assert !isUndefined(var);
    undefined = undefined.add(var);
  }

  private void addUndefined (VarSet partition) {
    undefined = undefined.union(partition);
  }

  /**
   * Remove the given variable from the set of undefined variables
   */
  void removeFromUndefined (NumVar var) {
    undefined = undefined.remove(var);
  }

  /**
   * Remove the variable from any partition, i.e. any association of the variable with a flag.
   */
  protected void removeFromPartition (NumVar variable) {
    FlagVar flag = getFlagFor(variable).getOrNull();
    if (flag != null) {
      reverse = reverse.remove(variable);
      VarSet partition = partitions.get(flag).get();
      partition = partition.remove(variable);
      if (partition.isEmpty()) {
        partitions = partitions.remove(flag);
        getChildOps().addKill(flag);
      } else {
        partitions = partitions.bind(flag, partition);
      }
    }
  }

  /**
   * Removes a variable from its partition but does not kill the flag variable
   * of the partition even if the partition would become empty after the removal.
   */
  private void removeFromPartitionNoKill (NumVar variable) {
    FlagVar flag = getFlagFor(variable).getOrNull();
    if (flag != null) {
      reverse = reverse.remove(variable);
      VarSet partition = partitions.get(flag).get();
      partition = partition.remove(variable);
      if (partition.isEmpty()) {
        partitions = partitions.remove(flag);
      } else {
        partitions = partitions.bind(flag, partition);
      }
    }
  }

  /**
   * Removes a variable from its partition but does not kill the flag variable
   * of the partition even if the partition would become empty after the removal.
   */
  private void removeFromPartitionNoKill (FlagVar flag, VarSet variables) {
    VarSet partition = partitions.get(flag).get();
    assert partition.containsAll(variables);
    for (NumVar variable : variables) {
      reverse = reverse.remove(variable);
    }
    partition = partition.difference(variables);
    if (partition.isEmpty())
      partitions = partitions.remove(flag);
    else
      partitions = partitions.bind(flag, partition);
  }

  private void removePartition (FlagVar flag) {
    if (!isFlag(flag))
      return;
    VarSet partition = partitions.get(flag).get();
    partitions = partitions.remove(flag);
    for (NumVar var : partition)
      reverse = reverse.remove(var);
  }

  /**
   * Adds a variable into a new partition associated with a new flag.
   * The flag is initially set to {@code 0} meaning the partition is undefined.
   *
   * @param singletonPartition A variable that should form a new partition
   * @return The fresh flag associated with the partition
   */
  protected FlagVar addPartition (NumVar singletonPartition) {
    return addPartition(VarSet.of(singletonPartition));
  }

  /**
   * Add a new partition that does not contain any variable.
   * Needed sometimes when we have to operate on the partition flag in the child
   * and only afterwards want to add variables to the partition.
   */
  protected FlagVar addEmptyPartition () {
    return addPartition(VarSet.empty());
  }

  /**
   * Adds a new partition associated with a new flag.
   * The flag is initially set to {@code 0} meaning the partition is undefined.
   *
   * @param partition A set of variables that should form a new partition
   * @return The fresh flag associated with the partition
   */
  private FlagVar addPartition (VarSet partition) {
    FlagVar flag = freshFlag();
    addNewPartition(flag, partition);
    getChildOps().addIntroZero(flag);
    return flag;
  }

  private void addNewPartition (FlagVar flag, VarSet partition) {
    assert partitions.get(flag).isNone();
    partitions = partitions.bind(flag, partition);
    for (NumVar var : partition)
      reverse = reverse.bind(var, flag);
  }

  protected void addToPartition (FlagVar flag, NumVar variable) {
    VarSet partition = partitions.get(flag).getOrElse(VarSet.empty());
    partitions = partitions.bind(flag, partition.add(variable));
    reverse = reverse.bind(variable, flag);
  }

  /**
   * Remove the variable from any partition and project it out in the child.
   */
  protected void project (NumVar variable) {
    if (isUndefined(variable)) {
      removeFromUndefined(variable);
    } else {
      removeFromPartition(variable);
      getChildOps().addKill(variable);
    }
  }

  /**
   * Introduce the variable in the child with top as value if it was undefined.
   */
  protected void promoteToChild (NumVar variable) {
    promoteToChild(VarSet.of(variable));
  }

  /**
   * Introduce the variables that are undefined in the child with top as value.
   */
  protected void promoteToChild (VarSet vars) {
    VarSet toPromote = vars.intersection(undefined);
    if (!toPromote.isEmpty()) {
      undefined = undefined.difference(toPromote);
      for (NumVar var : toPromote)
        getChildOps().addIntro(var);
    }
  }

  /**
   * Return all flags associated with the given variables.
   *
   * @param vars the variables mentioned in a statement
   * @return any flags associated with the variables
   */
  public VarSet getFlagsFor (VarSet vars) {
    VarSet flags = VarSet.empty();
    for (NumVar var : vars) {
      Option<FlagVar> flag = getFlagFor(var);
      if (flag.isSome())
        flags = flags.add(flag.get());
    }
    return flags;
  }

  Option<FlagVar> getFlagFor (NumVar var) {
    return reverse.get(var);
  }

  /**
   * Return the flag associated with the given variable if any.
   */
  public VarSet getFlagsFor (NumVar var) {
    return getFlagsFor(VarSet.empty().add(var));
  }

  public UndefState build () {
    return new UndefState(undefined, partitions, reverse);
  }

  public void substitute (NumVar x, NumVar y) {
    if (isUndefined(x)) {
      removeFromUndefined(x);
      addUndefined(y);
    } else {
      getChildOps().addSubst(x, y);
      FlagVar flag = getFlagFor(x).getOrNull();
      if (flag != null) {
        VarSet part = partitions.get(flag).get();
        part = part.remove(x).add(y);
        reverse = reverse.remove(x).bind(y, flag);
        partitions = partitions.bind(flag, part);
      }
    }
  }

  /**
   * Divide undefined and the partitions of this such that both (this and other) associate the maximum number of
   * variables with a common flag. Modifies both builders and children accordingly.
   * Expects that the support set has been made compatible before, i.e. both states contain the same variables.
   */
  protected <D extends FiniteDomain<D>> void makeCompatible (D childState, UndefStateBuilder other, D otherChildState) {
    makeUndefinedCompatible(childState, other, otherChildState);
    makePartitionsCompatible(other);
  }

  private <D extends FiniteDomain<D>> void makeUndefinedCompatible (D childState, UndefStateBuilder other,
      D otherChildState) {
    P3<VarSet, VarSet, VarSet> split = this.undefined.split(other.undefined);
    VarSet undefinedInFst = split._1();
    VarSet inBoth = split._2();
    VarSet undefinedInSnd = split._3();
    this.undefined = inBoth;
    other.undefined = inBoth;
    // As both sides have the same support set, undefined vars present only in this
    // are either defined or in a partition in other. Hence, they will form a new partition in this.
    if (!undefinedInFst.isEmpty()) {
      this.getChildOps().addCopyAndPaste(undefinedInFst, otherChildState);
      this.addPartition(undefinedInFst);
    }
    // and vice versa
    if (!undefinedInSnd.isEmpty()) {
      other.getChildOps().addCopyAndPaste(undefinedInSnd, childState);
      other.addPartition(undefinedInSnd);
    }
  }

  private void makePartitionsCompatible (UndefStateBuilder other) {
    // the set of undefined variables has been made compatible in both before we come here,
    // so if a variable does not exist in a partition it is definitely defined
    ThreeWaySplit<AVLMap<FlagVar, VarSet>> mapSplit = partitions.split(other.partitions);
    AVLMap<FlagVar, VarSet> onlyInFirst = mapSplit.onlyInFirst();
    AVLMap<FlagVar, VarSet> onlyInSecond = mapSplit.onlyInSecond();

    for (P2<FlagVar, VarSet> inBoth : mapSplit.inBothButDiffering()) {
      FlagVar commonFlag = inBoth._1();
      VarSet thisVars = inBoth._2();
      VarSet otherVars = other.partitions.get(commonFlag).get();
      P3<VarSet, VarSet, VarSet> split = thisVars.split(otherVars);
      VarSet onlyInThis = split._1();
      VarSet common = split._2();
      VarSet onlyInOther = split._3();
      // create new partitions for the not common vars and insert them in the right split to be processed later on
      if (!onlyInThis.isEmpty()) {
        FlagVar newPartitionFlag = splitPartition(this, commonFlag, onlyInThis);
        onlyInFirst = onlyInFirst.bind(newPartitionFlag, onlyInThis);
      }
      if (!onlyInOther.isEmpty()) {
        FlagVar newPartitionFlag = splitPartition(other, commonFlag, onlyInOther);
        onlyInSecond = onlyInSecond.bind(newPartitionFlag, onlyInOther);
      }
      removePartition(commonFlag);
      other.removePartition(commonFlag);
      if (!common.isEmpty()) {
        // keep the common flag for the common vars
        addNewPartition(commonFlag, common);
        other.addNewPartition(commonFlag, common);
      } else {
        getChildOps().addKill(commonFlag);
        other.getChildOps().addKill(commonFlag);
      }
    }

    // as a preprocessing step lift the variables that are defined on one side and in a partition on the other side
    // into a new partition thus later on we only have to deal with matching flags of different partitions
    VarSet definedInOther = VarSet.empty();
    for (P2<FlagVar, VarSet> thisBinding : onlyInFirst) {
      for (NumVar variable : thisBinding._2()) {
        if (other.isDefined(variable)) {
          definedInOther = definedInOther.add(variable);
        }
      }
    }
    if (!definedInOther.isEmpty()) {
      FlagVar definedInOtherFlag = freshFlag();
      other.getChildOps().addIntro(definedInOtherFlag, Bound.ONE);
      other.addNewPartition(definedInOtherFlag, definedInOther);
    }
    VarSet definedInThis = VarSet.empty();
    for (P2<FlagVar, VarSet> otherBinding : onlyInSecond) {
      for (NumVar variable : otherBinding._2()) {
        if (this.isDefined(variable)) {
          definedInThis = definedInThis.add(variable);
        }
      }
    }
    if (!definedInThis.isEmpty()) {
      FlagVar definedInThisFlag = freshFlag();
      this.getChildOps().addIntro(definedInThisFlag, Bound.ONE);
      this.addNewPartition(definedInThisFlag, definedInThis);
    }

    // now split partitions such that only subsets from same partitions in both form a partition
    VarSet varsOfOnlyInThis = VarSet.empty();
    for (P2<FlagVar, VarSet> binding : onlyInFirst) {
      varsOfOnlyInThis = varsOfOnlyInThis.union(binding._2());
    }
    VarSet varsOfOnlyInOther = VarSet.empty();
    for (P2<FlagVar, VarSet> binding : onlyInSecond) {
      varsOfOnlyInOther = varsOfOnlyInOther.union(binding._2());
    }
    VarSet matched = VarSet.empty();
    matched = splitMatching(varsOfOnlyInThis, matched, other);
    matched = splitMatching(varsOfOnlyInOther, matched, other);
    assert matched.difference(varsOfOnlyInThis).difference(varsOfOnlyInOther).isEmpty();
  }

  private VarSet splitMatching (VarSet variablesToMatch, VarSet alreadyMatched, UndefStateBuilder other) {
    for (NumVar variable : variablesToMatch) {
      if (alreadyMatched.contains(variable))
        continue; // for performance do not process again the ones that are already matched
      // the variable might have been moved to a different partition in earlier iterations,
      // so we need to retrieve the flag and the partition again
      FlagVar thisFlag = this.reverse.get(variable).get();
      FlagVar otherFlag = other.reverse.get(variable).get();
      assert !otherFlag.equalTo(thisFlag); // the case of "in both but different" mappings has been handled in the very first for loop
      VarSet thisPartition = this.partitions.get(thisFlag).get();
      VarSet otherPartition = other.partitions.get(otherFlag).get();
      P3<VarSet, VarSet, VarSet> split = thisPartition.split(otherPartition);
      VarSet onlyInThis = split._1();
      VarSet common = split._2();
      VarSet onlyInOther = split._3();
      // the not matching rest will be put into own partitions and will be processed in next iterations
      splitPartition(this, thisFlag, onlyInThis);
      splitPartition(other, otherFlag, onlyInOther);
      // add the common ones to the set of already matching pairs
      alreadyMatched = alreadyMatched.union(common);
      // and use the same flag for the partition in both
      this.removePartition(thisFlag);
      this.addNewPartition(otherFlag, common);
      this.getChildOps().addSubst(thisFlag, otherFlag);
    }
    return alreadyMatched;
  }

  /**
   * Find all equalities of the form {@code flag=0} or {@code flag=1} and
   * remove the corresponding variable set.
   *
   * @param newEqualities the equalities that have been propagated up the synthesized channel from child domains
   */
  public void reduceFromNewEqualities (SetOfEquations newEqualities) {
    for (Linear eq : newEqualities) {
      if (eq.isSingleTerm()) {
        NumVar var = eq.getKey();
        if (!var.isFlag())
          continue;
        BigInt value = eq.getConstant();
        if (value.isEqualTo(Bound.MINUSONE)) { // flag - 1 == 0
          Option<VarSet> oPart = partitions.get((FlagVar) var);
          if (oPart.isSome()) {
            removePartition((FlagVar) var);
            getChildOps().addKill(var);
          }
        }
        // we could make variables undefined if the flag is constant zero,
        // but we don't in order not to re-introduce a new flag when this
        // state is joined later on with a state where the variable is defined
//        else if (value.isEqualTo(BigInt.ZERO)) {
//          Option<VarSet> partition = partitions.get((FlagVar) var);
//          if (partition.isSome()) {
//            unbind((FlagVar) var);
//            childOps.addKill(var);
//            for (NumVar v : partition.get())
//              childOps.addKill(v);
//            undefined = undefined.union(partition.get());
//          }
//        }
      }
    }
  }

  /**
   * Find all the partitions where the flag is constant and promote the variables in that partition.
   */
  public <D extends FiniteDomain<D>> void reduceWithQuery (D childState, VarSet flags) {
    for (NumVar flag : flags) {
      Range range = childState.queryRange(flag);
      if (range.isOne()) {
        removePartition((FlagVar) flag);
        getChildOps().addKill(flag);
      } else if (range.isZero()) {
        VarSet partition = partitions.get((FlagVar) flag).get();
        addUndefined(partition);
        removePartition((FlagVar) flag);
        getChildOps().addKill(flag);
        for (NumVar var : partition) {
          getChildOps().addKill(var);
        }
      }
    }
  }

  /**
   * Copy the state associated with the given variables from the passed in state to this state.
   *
   * @param vars A set of variables for which the state should be copied over
   * @param otherState The state for the given variables
   * @param otherChildState The child state for the given variables
   */
  protected <D extends FiniteDomain<D>> void copyAndPaste (VarSet vars, UndefState otherState, D otherChildState) {
    VarSet childCopyVars = vars;
    for (NumVar var : vars) {
      if (contains(var))
        throw new VariableSupportSetException(); // the variables to be copied must not exist in this state
      if (otherState.undefined.contains(var)) {
        childCopyVars = childCopyVars.remove(var);
        addUndefined(var);
      }
    }
    VarSet otherFlags = new UndefStateBuilder(otherState).getFlagsFor(childCopyVars);
    for (NumVar otherFlagVar : otherFlags) {
      FlagVar otherFlag = (FlagVar) otherFlagVar;
      VarSet otherPartition = otherState.partitions.getOrNull(otherFlag);
      if (isFlag(otherFlag)) {
        // the same variable exists as a flag in this. We need to rename it before copying it over to avoid a name clash
        FlagVar renamedOtherFlag = freshFlag();
        otherChildState = otherChildState.substitute(otherFlag, renamedOtherFlag);
        otherFlag = renamedOtherFlag;
      }
      addNewPartition(otherFlag, otherPartition.intersection(vars));
      childCopyVars = childCopyVars.add(otherFlag);
    }
    getChildOps().addCopyAndPaste(childCopyVars, otherChildState);
  }

  protected void fold (ListVarPair pairs) {
    ListVarPair childVars = foldNG(pairs);
    getChildOps().addFoldNG(childVars);
  }

  ListVarPair foldNG (ListVarPair pairs) {
    // these flags will be removed if not used below
    FlagVar undefinedFlag = freshFlag();
    getChildOps().addIntroZero(undefinedFlag);
    FlagVar definedFlag = freshFlag();
    getChildOps().addIntro(definedFlag, Bound.ONE);
    VarSet permanents = VarSet.empty();
    VarSet ephemerals = VarSet.empty();
    ListVarPair childVars = new ListVarPair();
    ListVarPair partitionVars = new ListVarPair();
    Map<NumVar, NumVar> forwardPairs = new HashMap<NumVar, NumVar>();
    Map<NumVar, NumVar> backwardPairs = new HashMap<NumVar, NumVar>();
    // some preprocessing sorting out the simple cases
    for (VarPair pair : pairs) {
      NumVar permanentVar = pair.getPermanent();
      NumVar ephemeralVar = pair.getEphemeral();
      // pairs with both variables undefined are folded by removing the ephemeral
      if (isUndefined(permanentVar) && isUndefined(ephemeralVar)) {
        removeFromUndefined(ephemeralVar);
        continue;
      }
      // variables that are not tracked here are passed on to the child
      if (isDefined(permanentVar) && isDefined(ephemeralVar)) {
        childVars.add(pair);
        continue;
      }
      // put all the undefined vars into an own partition with flag=0
      if (isUndefined(permanentVar)) {
        removeFromUndefined(permanentVar);
        addToPartition(undefinedFlag, permanentVar);
        ListVarPair singleton = new ListVarPair();
        singleton.add(ephemeralVar, permanentVar);
        getChildOps().addExpandNG(singleton);
      } else if (isUndefined(ephemeralVar)) {
        removeFromUndefined(ephemeralVar);
        addToPartition(undefinedFlag, ephemeralVar);
        ListVarPair singleton = new ListVarPair();
        singleton.add(permanentVar, ephemeralVar);
        getChildOps().addExpandNG(singleton);
      }
      // put all defined vars into an own partition with flag=1
      if (isDefined(permanentVar)) {
        addToPartition(definedFlag, permanentVar);
      } else if (isDefined(ephemeralVar)) {
        addToPartition(definedFlag, ephemeralVar);
      }
      // record the above for later processing
      partitionVars.add(pair);
      forwardPairs.put(permanentVar, ephemeralVar);
      backwardPairs.put(ephemeralVar, permanentVar);
      permanents = permanents.add(permanentVar);
      ephemerals = ephemerals.add(ephemeralVar);
    }
    // if the flags for defined/undefined were not used then remove them again
    if (partitions.get(definedFlag).isNone())
      getChildOps().addKill(definedFlag);
    if (partitions.get(undefinedFlag).isNone())
      getChildOps().addKill(undefinedFlag);
    // all unhandled variable pairs are now in partitions
    // next split partitions such that permanent and ephemeral variables are always separated
    VarSet permanentFlags = getFlagsFor(permanents);
    VarSet ephemeralFlags = getFlagsFor(ephemerals);
    VarSet mixedPartitions = permanentFlags.intersection(ephemeralFlags);
    for (NumVar flag : mixedPartitions) {
      FlagVar partitionFlag = (FlagVar) flag;
      VarSet partition = partitions.get(partitionFlag).get();
      VarSet ephemeralsInPartition = partition.intersection(ephemerals);
      splitPartition(this, partitionFlag, ephemeralsInPartition);
    }
    // split partitions such that only pairs from same partitions are in a partition
    VarSet matched = VarSet.empty();
    for (VarPair pair : partitionVars) {
      NumVar permanentVar = pair.getPermanent();
      if (matched.contains(permanentVar))
        continue; // for performance do not process again the ones that are already matched
      NumVar ephemeralVar = pair.getEphemeral();
      FlagVar permanentFlag = getFlagFor(permanentVar).get();
      FlagVar ephemeralFlag = getFlagFor(ephemeralVar).get();
      VarSet permanentPartition = partitions.get(permanentFlag).get();
      VarSet ephemeralPartition = partitions.get(ephemeralFlag).get();
      P3<VarSet, VarSet, VarSet> split =
        splitWithMapping(permanentPartition, ephemeralPartition, forwardPairs, backwardPairs);
      VarSet onlyInPermanent = split._1();
      VarSet common = split._2();
      VarSet onlyInEphemeral = split._3();
      // add the common ones to the set of already matching pairs
      matched = matched.union(common);
      // the rest will be put into own partitions and will be processed in next iterations
      splitPartition(this, permanentFlag, onlyInPermanent);
      splitPartition(this, ephemeralFlag, onlyInEphemeral);
    }
    // fold the variable pairs and the flag pairs
    FoldMap childMap = FoldMap.fromList(childVars);
    for (VarPair pair : partitionVars) {
      childVars.add(pair);
      childMap.add(pair);
      NumVar permanentVar = pair.getPermanent();
      NumVar ephemeralVar = pair.getEphemeral();
      FlagVar permanentFlag = getFlagFor(permanentVar).get();
      FlagVar ephemeralFlag = getFlagFor(ephemeralVar).get();
      if (!permanentFlag.equalTo(ephemeralFlag)) {
        removeFromPartitionNoKill(ephemeralVar);
        if (childMap.isPermanent(permanentFlag)) // avoid re-adding flags
          assert childMap.getEphemeral(permanentFlag).equalTo(ephemeralFlag);
        else {
          childVars.add(permanentFlag, ephemeralFlag);
          childMap.add(permanentFlag, ephemeralFlag);
        }
      }
    }
    return childVars;
  }

  /**
   * Split a partition such that the given variables form a new partition. Note that if the fragment to be split
   * is the whole partition itself this function makes a new partition for the fragment.
   *
   * @param builder The builder on which to perform the operations
   * @param partitionFlag The flag for the partition to split
   * @param fragment The variables in the partition that should form a new partition
   * @return The flag of the new partition containing the fragment
   */
  private static FlagVar splitPartition (UndefStateBuilder builder, FlagVar partitionFlag, VarSet fragment) {
    if (fragment.isEmpty())
      return partitionFlag;
    VarSet partition = builder.partitions.get(partitionFlag).get();
    assert partition.containsAll(fragment);
    builder.removeFromPartitionNoKill(partitionFlag, fragment);
    FlagVar newPartitionFlag = freshFlag();
    copyFlag(builder, partitionFlag, newPartitionFlag, fragment);
    return newPartitionFlag;
  }

  /**
   * Split up two sets such that we get the parts only in first, the intersection and the parts only in second.
   * This split uses a mapping between variables because first and second do not contain the same variables
   * but a mapping between them. The split should be performed according to this mapping where {a} intersect {b} = {a}
   * if we have the mapping a -> b. Thus the common part is taken from the first set.
   */
  private static P3<VarSet, VarSet, VarSet> splitWithMapping (VarSet first, VarSet second,
      Map<NumVar, NumVar> forwardPairs, Map<NumVar, NumVar> backwardPairs) {
    VarSet common = first.intersection(second.substitute(backwardPairs));
    VarSet onlyInFirst = first.difference(common);
    VarSet onlyInSecond = second.difference(common.substitute(forwardPairs));
    return P3.tuple3(onlyInFirst, common, onlyInSecond);
  }

  /**
   * Copy the state of the flag to a new flag that will be associated with the variables.
   *
   * @param builder The builder on which to perform the operations
   * @param fromFlag A flag of which the state is to be copied
   * @param toFlag A flag to which state will be copied to
   * @param variables A partition that should be associated with a copy of the flag. Usually the partition associated
   *          with the {@code fromFlag}
   */
  private static void copyFlag (UndefStateBuilder builder, FlagVar fromFlag, FlagVar toFlag, VarSet variables) {
    ListVarPair singleton = new ListVarPair();
    singleton.add(fromFlag, toFlag);
    builder.getChildOps().addExpandNG(singleton);
    builder.addNewPartition(toFlag, variables);
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    if (getChildOps().length() > 0)
      builder.append("Childops:").append(getChildOps()).append("\n");
    builder.append(build().toString());
    return builder.toString();
  }

  public void expandNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    ListVarPair cvps = expandOnState(nvps);
    getChildOps().addExpandNG(p, e, cvps);
  }

  public void expandNG (ListVarPair nvps) {
    ListVarPair cvps = expandOnState(nvps);
    getChildOps().addExpandNG(cvps);
  }

  private ListVarPair expandOnState (ListVarPair nvps) {
    ListVarPair cvps = new ListVarPair();
    for (VarPair pair : nvps) {
      NumVar permanentVar = pair.getPermanent();
      NumVar ephemeralVar = pair.getEphemeral();
      if (contains(ephemeralVar))
        throw new VariableSupportSetException();
      if (isUndefined(permanentVar)) {
        addUndefined(ephemeralVar);
        continue;
      }
      // we do not track the variable here so just pass the operation on to the child
      if (isDefined(permanentVar)) {
        cvps.add(pair);
        continue;
      }
      FlagVar flag = reverse.getOrNull(permanentVar);
      VarSet partition = partitions.getOrNull(flag);
      removePartition(flag);
      addNewPartition(flag, partition.add(ephemeralVar));
      cvps.add(pair);
    }
    return cvps;
  }
}
